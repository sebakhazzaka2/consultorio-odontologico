# Configuración de Notificaciones por Email

Para que las notificaciones de citas funcionen correctamente, necesitas configurar EmailJS.

## Pasos para Configurar EmailJS

### 1. Crear cuenta en EmailJS
1. Ve a https://www.emailjs.com/
2. Crea una cuenta gratuita (permite hasta 200 emails/mes)
3. Verifica tu email

### 2. Crear un Email Service
1. En el dashboard de EmailJS, ve a **Email Services**
2. Haz clic en **Add New Service**
3. Selecciona tu proveedor de email (Gmail, Outlook, etc.)
4. Sigue las instrucciones para conectar tu cuenta
5. **Guarda el Service ID** (ejemplo: `service_xxxxx`)

### 3. Crear un Email Template
1. Ve a **Email Templates**
2. Haz clic en **Create New Template**
3. Usa este template como base:

**Subject:**
```
Nueva Cita Agendada - {{servicio}}
```

**Content (HTML):**
```html
<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background: #667eea; color: white; padding: 20px; text-align: center; }
        .content { background: #f9f9f9; padding: 20px; margin-top: 20px; }
        .info-row { margin: 10px 0; padding: 10px; background: white; border-left: 3px solid #667eea; }
        .label { font-weight: bold; color: #667eea; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Nueva Cita Agendada</h1>
        </div>
        <div class="content">
            <h2>Detalles de la Cita</h2>
            
            <div class="info-row">
                <span class="label">Paciente:</span> {{paciente_nombre}}
            </div>
            
            <div class="info-row">
                <span class="label">Email:</span> {{paciente_email}}
            </div>
            
            <div class="info-row">
                <span class="label">Teléfono:</span> {{paciente_telefono}}
            </div>
            
            <div class="info-row">
                <span class="label">Servicio:</span> {{servicio}}
            </div>
            
            <div class="info-row">
                <span class="label">Fecha:</span> {{fecha}}
            </div>
            
            <div class="info-row">
                <span class="label">Horario:</span> {{horario}}
            </div>
            
            <div class="info-row">
                <span class="label">Motivo:</span> {{motivo}}
            </div>
        </div>
    </div>
</body>
</html>
```

4. **Guarda el Template ID** (ejemplo: `template_xxxxx`)

### 4. Obtener tu Public Key
1. Ve a **Account** → **General**
2. Copia tu **Public Key** (ejemplo: `xxxxxxxxxxxxx`)

### 5. Configurar en el Código
1. Abre el archivo: `src/app/services/email-notification.service.ts`
2. Reemplaza los siguientes valores:

```typescript
private readonly PUBLIC_KEY = 'TU_PUBLIC_KEY_AQUI';
private readonly SERVICE_ID = 'TU_SERVICE_ID_AQUI';
private readonly TEMPLATE_ID = 'TU_TEMPLATE_ID_AQUI';
```

### 6. Verificar que el Email Destino esté correcto
El email destino ya está configurado como: `faacubp.27@hotmail.com`

Si necesitas cambiarlo, edita la línea en `email-notification.service.ts`:
```typescript
private readonly EMAIL_DESTINO = 'faacubp.27@hotmail.com';
```

## Variables Disponibles en el Template

- `{{paciente_nombre}}` - Nombre completo del paciente
- `{{paciente_email}}` - Email del paciente
- `{{paciente_telefono}}` - Teléfono del paciente
- `{{servicio}}` - Nombre del servicio solicitado
- `{{fecha}}` - Fecha formateada (ej: "lunes, 15 de enero de 2024")
- `{{horario}}` - Hora de la cita (ej: "10:00")
- `{{motivo}}` - Motivo de la consulta (si fue proporcionado)

## Prueba

Una vez configurado, cuando alguien agende una cita, recibirás un email en `faacubp.27@hotmail.com` con todos los detalles.

## Nota de Seguridad

Para producción, considera:
- Mover las credenciales a variables de entorno
- Implementar un backend propio para mayor seguridad
- Usar un servicio de email más robusto

