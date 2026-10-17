# Importación de pacientes desde CSV

Carga masiva de pacientes/clientas al dar de alta una instancia (por ejemplo, desde un export de Ágora o una planilla de Excel). v1 sin interfaz: un endpoint de admin que se corre con `curl` durante el onboarding.

- Endpoint: `POST /api/pacientes/importar?confirmar=false|true` (default `false`), multipart con el campo `archivo`. Requiere JWT de admin.
- Código: `backend/.../service/importacion/` (`CsvLector`, `MapeoColumnas`, `PacienteImportService`).
- Plantilla: [`plantilla-importacion-pacientes.csv`](plantilla-importacion-pacientes.csv).

## Dos pasos, sin estado en el servidor

1. **Simulación** (`confirmar=false`): analiza el archivo y devuelve el estado de cada fila. No escribe nada.
2. **Confirmación** (`confirmar=true`): se sube **el mismo archivo** otra vez; se vuelve a analizar todo y se insertan solo las filas `OK`, con `creado_por = IMPORT`, en una sola transacción.

Correr el mismo archivo dos veces no duplica nada: la segunda vez todas las filas insertadas salen como `YA_EXISTE`.

## Formato del archivo

- **Codificación:** UTF-8 (con o sin BOM) o Windows-1252/Latin-1 (lo que guarda Excel en español). Se detecta sola.
- **Separador:** `;`, `,` o tabulador. Se detecta en la línea de encabezado.
- **Encabezado obligatorio** en la primera línea. Las columnas que no se reconocen se ignoran.
- **Filas vacías** se saltean. Máximo **5000 filas** de datos y 5 MB por archivo.
- Las celdas se recortan (espacios alrededor).

### Columnas y alias

Los encabezados se comparan sin tildes, sin mayúsculas y con `_` como espacio (`Teléfono`, `TELEFONO` y `telefono` son lo mismo).

| Campo | Alias aceptados | Obligatorio |
|-------|-----------------|-------------|
| Nombre | `nombre`, `nombres`, `first name` | sí, junto con apellido (o usar nombre completo) |
| Apellido | `apellido`, `apellidos`, `last name` | sí, junto con nombre (o usar nombre completo) |
| Nombre completo | `nombre completo`, `cliente`, `clienta`, `paciente`, `nombre y apellido` | alternativa a nombre + apellido |
| Teléfono | `telefono`, `teléfono`, `celular`, `movil`, `móvil`, `whatsapp`, `tel` | sí |
| Email | `email`, `e-mail`, `correo`, `mail`, `correo electronico` | no |
| Fecha de nacimiento | `fecha de nacimiento`, `nacimiento`, `fecha nacimiento`, `fecha_nacimiento`, `cumpleaños` | no |

Si falta el teléfono o el nombre, el endpoint responde **400** con la lista de encabezados detectados. Para un export nuevo (por ejemplo Ágora), se suman alias en `MapeoColumnas.ALIAS`.

### Reglas por fila

- **Nombre:** si hay columnas nombre y apellido, se usan. Si no, el nombre completo se separa en el **último espacio** (`Ana María López` → nombre `Ana María`, apellido `López`). Un nombre de una sola palabra queda en `REVISAR`. Los apellidos compuestos (`Ana de León`) quedan mal separados: en ese caso conviene usar columnas separadas.
- **Teléfono:** obligatorio. Se normaliza con `PhoneNormalizer` según `CLINIC_COUNTRY` (598 Uruguay, 55 Brasil). Acepta espacios, guiones, puntos, paréntesis, `+` y `00`.
  - **Qué se guarda:** los dígitos tal como los escribió la persona (`099 572 537` → `099572537`), igual que los pacientes cargados a mano hoy (formato local). Si el número vino con prefijo internacional (`+` o `00`), se guarda en E.164 sin `+` (`59899572537`). La comparación de duplicados usa siempre el E.164, así que `099572537` y `+598 99 572 537` son el mismo teléfono. La migración de todos los teléfonos a E.164 queda para F2a.
- **Email:** opcional. Se guarda en minúsculas y sin espacios. Formato inválido (o dominio sin punto) → `ERROR`. **Nunca** se inventan emails de relleno.
- **Fecha de nacimiento:** opcional. `dd/MM/aaaa` (también `d/M/aaaa` y `aaaa-MM-dd`). Fecha inexistente o futura → `ERROR`.
- **Duplicados:** por email normalizado; si la fila no tiene email, por teléfono E.164. **Nunca por nombre** (un dedupe por nombre fue un bug revertido en el PR #34).

### Estados

| Estado | Significado | ¿Se inserta al confirmar? |
|--------|-------------|---------------------------|
| `OK` | Válida y nueva | sí |
| `ERROR` | Falta un dato obligatorio o hay uno inválido (motivo en la fila) | no |
| `REVISAR` | Válida pero dudosa (nombre de una sola palabra) | no; corregir y volver a importar |
| `DUPLICADO_ARCHIVO` | Mismo email (o, sin email, mismo teléfono) que una fila anterior del archivo; el motivo indica cuál | no |
| `YA_EXISTE` | Ya hay un paciente con ese email (o, sin email, con ese teléfono) | no |

`fila` es el número de fila de datos, empezando en 1 y **sin contar el encabezado**: la fila 1 es la segunda línea del archivo (la fila 2 de Excel). Las filas vacías cuentan para la numeración.

Por privacidad, la respuesta no repite nombres, teléfonos ni emails (solo número de fila, estado y motivo) y los logs registran solo conteos.

### Respuesta

```json
{
  "total": 3, "ok": 1, "errores": 1, "revisar": 1, "duplicados": 0, "ya_existen": 0,
  "insertados": 0, "confirmado": false,
  "filas": [
    { "fila": 1, "estado": "OK", "motivo": null },
    { "fila": 2, "estado": "ERROR", "motivo": "Teléfono inválido para el país de la instancia" },
    { "fila": 3, "estado": "REVISAR", "motivo": "Falta el apellido (el nombre tiene una sola palabra o la columna está vacía)" }
  ]
}
```

## Cómo correrlo

```bash
DOMINIO=https://<subdominio>.turnosuy.com

# 1. JWT de admin
TOKEN=$(curl -s -X POST "$DOMINIO/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"<admin>","password":"<password>"}' | jq -r .token)

# 2. Simulación (no escribe nada)
curl -s -F archivo=@clientas.csv -H "Authorization: Bearer $TOKEN" \
  "$DOMINIO/api/pacientes/importar?confirmar=false" | jq

# 3. Confirmación (mismo archivo)
curl -s -F archivo=@clientas.csv -H "Authorization: Bearer $TOKEN" \
  "$DOMINIO/api/pacientes/importar?confirmar=true" | jq
```

El login tiene rate limit (5 por minuto) y el JWT dura 2 h en prod.

### Flujo recomendado

1. Simulación.
2. Corregir el CSV según los motivos (`ERROR` y `REVISAR`); revisar los `DUPLICADO_ARCHIVO` por si son personas distintas que comparten email o teléfono (cargarlas a mano).
3. Repetir la simulación hasta que no queden errores sin explicar.
4. Confirmación.
5. Volver a correr la simulación: todas las filas insertadas tienen que salir `YA_EXISTE`.

## Errores del archivo completo

| Respuesta | Causa |
|-----------|-------|
| 400 | Archivo vacío, sin encabezado, sin columnas obligatorias, más de 5000 filas, CSV mal formado (comillas sin cerrar) o falta el campo `archivo` |
| 401 | Sin JWT o JWT vencido |
| 409 | Al confirmar, alguien creó en paralelo un paciente con un email del archivo. La transacción se revierte entera (no queda nada insertado); volver a correr: esa fila sale `YA_EXISTE` |
| 413 | Archivo de más de 5 MB |

## Rollback

Los pacientes importados quedan marcados con `creado_por = 'IMPORT'`. Si una importación salió mal y todavía no tienen citas, historial ni pagos asociados:

```sql
SELECT COUNT(*) FROM pacientes WHERE creado_por = 'IMPORT';
-- revisar el número antes de borrar
DELETE FROM pacientes WHERE creado_por = 'IMPORT';
```

Hacer un backup antes (`scripts/backup.sh`). Las FKs con `ON DELETE RESTRICT` impiden borrar pacientes que ya tienen datos asociados.
