const request = require('supertest');
const app = require('./server');

describe('GET /', () => {
  it('responde con mensaje de estado', async () => {
    const res = await request(app).get('/');
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty('message');
    expect(res.body.message).toContain('funcionando');
  });
});
