const db = require('../config/db');
const { validarCita } = require('../validators/citas.validator');

/**
 * Comprueba si ya existe una cita en la misma fecha y hora.
 */
async function existeCitaEnHorario(fecha, hora, excluirId = null) {
  let sql = 'SELECT id FROM citas WHERE fecha = ? AND hora = ?';
  const params = [fecha, hora];
  if (excluirId != null) {
    sql += ' AND id != ?';
    params.push(excluirId);
  }
  const [rows] = await db.execute(sql, params);
  return rows.length > 0;
}

const CITA_CON_PACIENTE = `
  SELECT c.id, c.paciente_id, c.fecha, c.hora, c.motivo, c.estado, c.created_at,
         p.nombre AS paciente_nombre, p.apellido AS paciente_apellido, p.telefono AS paciente_telefono, p.email AS paciente_email
  FROM citas c
  INNER JOIN pacientes p ON p.id = c.paciente_id
`;

function mapearFilaCita(row) {
  return {
    id: row.id,
    paciente_id: row.paciente_id,
    fecha: row.fecha,
    hora: row.hora,
    motivo: row.motivo,
    estado: row.estado,
    created_at: row.created_at,
    paciente: {
      id: row.paciente_id,
      nombre: row.paciente_nombre,
      apellido: row.paciente_apellido,
      telefono: row.paciente_telefono,
      email: row.paciente_email
    }
  };
}

exports.obtenerCitas = async (req, res) => {
  try {
    const [rows] = await db.execute(`${CITA_CON_PACIENTE} ORDER BY c.fecha DESC, c.hora ASC`);
    res.json(rows.map(mapearFilaCita));
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al obtener citas' });
  }
};

exports.obtenerCitaPorId = async (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    if (isNaN(id) || id < 1) {
      return res.status(400).json({ error: 'ID de cita inválido' });
    }
    const [rows] = await db.execute(`${CITA_CON_PACIENTE} WHERE c.id = ?`, [id]);
    if (rows.length === 0) {
      return res.status(404).json({ error: 'Cita no encontrada' });
    }
    res.json(mapearFilaCita(rows[0]));
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al obtener la cita' });
  }
};

exports.crearCita = async (req, res) => {
  try {
    const resultado = validarCita(req.body);
    if (!resultado.valido) {
      return res.status(400).json({
        error: 'Error de validación',
        detalles: resultado.errores
      });
    }

    const { paciente_id, fecha, hora, motivo, estado } = resultado.bodyNormalizado;

    const [existePaciente] = await db.execute('SELECT id FROM pacientes WHERE id = ?', [paciente_id]);
    if (existePaciente.length === 0) {
      return res.status(400).json({ error: 'Paciente no encontrado', detalles: ['El paciente seleccionado no existe'] });
    }

    const ocupado = await existeCitaEnHorario(fecha, hora);
    if (ocupado) {
      return res.status(409).json({
        error: 'Ya existe una cita en ese horario',
        detalles: ['No se permiten dos turnos en la misma fecha y hora']
      });
    }

    const sql = `
      INSERT INTO citas (paciente_id, fecha, hora, motivo, estado)
      VALUES (?, ?, ?, ?, ?)
    `;
    await db.execute(sql, [paciente_id, fecha, hora, motivo || null, estado]);

    res.status(201).json({ message: 'Cita creada correctamente' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al crear la cita' });
  }
};

exports.actualizarCita = async (req, res) => {
  try {
    const id = parseInt(req.params.id, 10);
    if (isNaN(id) || id < 1) {
      return res.status(400).json({ error: 'ID de cita inválido' });
    }

    const resultado = validarCita(req.body);
    if (!resultado.valido) {
      return res.status(400).json({
        error: 'Error de validación',
        detalles: resultado.errores
      });
    }

    const { paciente_id, fecha, hora, motivo, estado } = resultado.bodyNormalizado;

    const [existentes] = await db.execute('SELECT id FROM citas WHERE id = ?', [id]);
    if (existentes.length === 0) {
      return res.status(404).json({ error: 'Cita no encontrada' });
    }

    const [existePaciente] = await db.execute('SELECT id FROM pacientes WHERE id = ?', [paciente_id]);
    if (existePaciente.length === 0) {
      return res.status(400).json({ error: 'Paciente no encontrado', detalles: ['El paciente seleccionado no existe'] });
    }

    const ocupado = await existeCitaEnHorario(fecha, hora, id);
    if (ocupado) {
      return res.status(409).json({
        error: 'Ya existe otra cita en ese horario',
        detalles: ['No se permiten dos turnos en la misma fecha y hora']
      });
    }

    const sql = `
      UPDATE citas SET paciente_id = ?, fecha = ?, hora = ?, motivo = ?, estado = ?
      WHERE id = ?
    `;
    await db.execute(sql, [paciente_id, fecha, hora, motivo || null, estado, id]);

    res.json({ message: 'Cita actualizada correctamente' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Error al actualizar la cita' });
  }
};

