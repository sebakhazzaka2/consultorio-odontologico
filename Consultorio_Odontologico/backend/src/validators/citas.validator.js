/**
 * Validaciones para el módulo de citas.
 * Cita: paciente_id, fecha, hora, motivo, estado (opcional).
 */

const FORMATO_FECHA = /^\d{4}-\d{2}-\d{2}$/;
const HORAS_PERMITIDAS = [
  '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
  '14:00', '14:30', '15:00', '15:30', '16:00', '16:30', '17:00', '17:30'
];
const ESTADOS_PERMITIDOS = ['pendiente', 'confirmada', 'cancelada'];

function validarFormatoFecha(fecha) {
  if (!fecha) return { valido: false, mensaje: 'Fecha no proporcionada' };
  let str;
  if (typeof fecha === 'string') {
    str = fecha.slice(0, 10);
  } else if (fecha && typeof fecha.toISOString === 'function') {
    str = fecha.toISOString().slice(0, 10);
  } else {
    return { valido: false, mensaje: 'Formato de fecha no válido' };
  }
  if (!FORMATO_FECHA.test(str)) {
    return { valido: false, mensaje: 'La fecha debe tener formato YYYY-MM-DD' };
  }
  return { valido: true, fechaStr: str };
}

function validarFechaNoPasada(fechaStr) {
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  const fecha = new Date(fechaStr + 'T00:00:00');
  if (isNaN(fecha.getTime())) {
    return { valido: false, mensaje: 'Fecha inválida' };
  }
  if (fecha < hoy) {
    return { valido: false, mensaje: 'No se pueden agendar citas en fechas pasadas' };
  }
  return { valido: true };
}

function validarHora(hora) {
  const h = String(hora).trim();
  if (!HORAS_PERMITIDAS.includes(h)) {
    return { valido: false, mensaje: 'Horario no válido. Use uno de: ' + HORAS_PERMITIDAS.join(', ') };
  }
  return { valido: true, hora: h };
}

/**
 * Valida el body para crear o actualizar una cita.
 * Acepta "horario" como alias de "hora".
 * @returns { { valido: boolean, errores: string[], bodyNormalizado?: object } }
 */
function validarCita(body) {
  const bodyNorm = { ...body };
  if (bodyNorm.horario != null && bodyNorm.hora == null) {
    bodyNorm.hora = bodyNorm.horario;
  }

  const errores = [];

  const pacienteId = bodyNorm.paciente_id != null ? parseInt(bodyNorm.paciente_id, 10) : NaN;
  if (isNaN(pacienteId) || pacienteId < 1) {
    errores.push('Debe seleccionar un paciente válido');
  }

  if (!bodyNorm.fecha) {
    errores.push('La fecha es obligatoria');
  } else {
    const rFecha = validarFormatoFecha(bodyNorm.fecha);
    if (!rFecha.valido) {
      errores.push(rFecha.mensaje);
    } else {
      const rNoPasada = validarFechaNoPasada(rFecha.fechaStr);
      if (!rNoPasada.valido) {
        errores.push(rNoPasada.mensaje);
      }
    }
  }

  if (!bodyNorm.hora && !bodyNorm.horario) {
    errores.push('El horario es obligatorio');
  } else {
    const rHora = validarHora(bodyNorm.hora || bodyNorm.horario);
    if (!rHora.valido) {
      errores.push(rHora.mensaje);
    }
  }

  let estado = (bodyNorm.estado != null ? String(bodyNorm.estado).trim().toLowerCase() : 'pendiente');
  if (!ESTADOS_PERMITIDOS.includes(estado)) {
    estado = 'pendiente';
  }

  if (errores.length > 0) {
    return { valido: false, errores };
  }

  const rFecha = validarFormatoFecha(bodyNorm.fecha);
  const rHora = validarHora(bodyNorm.hora || bodyNorm.horario);

  return {
    valido: true,
    bodyNormalizado: {
      paciente_id: pacienteId,
      fecha: rFecha.fechaStr,
      hora: rHora.hora,
      motivo: bodyNorm.motivo != null ? String(bodyNorm.motivo).trim() : null,
      estado
    }
  };
}

module.exports = {
  validarCita,
  validarFormatoFecha,
  validarFechaNoPasada,
  validarHora,
  HORAS_PERMITIDAS,
  ESTADOS_PERMITIDOS
};
