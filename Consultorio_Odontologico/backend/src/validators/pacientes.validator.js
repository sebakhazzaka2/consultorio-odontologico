/**
 * Validaciones para el módulo de pacientes.
 */

const FORMATO_FECHA = /^\d{4}-\d{2}-\d{2}$/;

function validarEmail(email) {
  const e = String(email).trim();
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!regex.test(e)) {
    return { valido: false, mensaje: 'El email no tiene un formato válido' };
  }
  return { valido: true };
}

function validarFechaNacimiento(fecha) {
  if (fecha == null || fecha === '') return { valido: true, fechaStr: null };
  const str = typeof fecha === 'string' ? fecha.slice(0, 10) : (fecha.toISOString ? fecha.toISOString().slice(0, 10) : '');
  if (!FORMATO_FECHA.test(str)) {
    return { valido: false, mensaje: 'La fecha de nacimiento debe tener formato YYYY-MM-DD' };
  }
  return { valido: true, fechaStr: str };
}

/**
 * Valida el body para crear o actualizar un paciente.
 * @returns { { valido: boolean, errores: string[], bodyNormalizado?: object } }
 */
function validarPaciente(body) {
  const errores = [];
  const nombre = body.nombre != null ? String(body.nombre).trim() : '';
  const apellido = body.apellido != null ? String(body.apellido).trim() : '';
  const telefono = body.telefono != null ? String(body.telefono).trim() : '';
  const email = body.email != null ? String(body.email).trim() : '';

  if (!nombre || nombre.length < 2) {
    errores.push('El nombre es obligatorio (mínimo 2 caracteres)');
  }
  if (!apellido || apellido.length < 2) {
    errores.push('El apellido es obligatorio (mínimo 2 caracteres)');
  }
  if (!telefono) {
    errores.push('El teléfono es obligatorio');
  }
  if (!email) {
    errores.push('El email es obligatorio');
  } else {
    const r = validarEmail(body.email);
    if (!r.valido) errores.push(r.mensaje);
  }

  const rFecha = validarFechaNacimiento(body.fecha_nacimiento);
  if (!rFecha.valido) {
    errores.push(rFecha.mensaje);
  }

  if (errores.length > 0) {
    return { valido: false, errores };
  }

  return {
    valido: true,
    bodyNormalizado: {
      nombre,
      apellido,
      telefono,
      email,
      fecha_nacimiento: rFecha.fechaStr || null
    }
  };
}

module.exports = { validarPaciente };
