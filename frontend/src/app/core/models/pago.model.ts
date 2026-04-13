export interface PagoPayload {
  paciente_id: number;
  monto: number;
  fecha: string;
  concepto?: string | null;
}

export interface Pago {
  id: number;
  paciente_id: number;
  nombre_paciente: string;
  apellido_paciente: string;
  monto: number;
  fecha: string;
  concepto: string | null;
  created_at: string;
}

export interface SaldoPaciente {
  paciente_id: number;
  nombre_paciente: string;
  apellido_paciente: string;
  total_deuda: number;
  total_pagado: number;
  saldo_pendiente: number;
}
