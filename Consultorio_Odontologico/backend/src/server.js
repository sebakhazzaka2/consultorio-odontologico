require('dotenv').config();
const express = require('express');
const cors = require('cors');
const citasRoutes = require('./routes/citas.routes');
const pacientesRoutes = require('./routes/pacientes.routes');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors({ origin: 'http://localhost:4200' }));
app.use(express.json());

app.get('/', (req, res) => {
  res.json({ message: 'Backend funcionando 🚀' });
});
app.use('/api/pacientes', pacientesRoutes);
app.use('/api/citas', citasRoutes);

app.listen(PORT, () => {
  console.log(`Servidor corriendo en http://localhost:${PORT}`);
});