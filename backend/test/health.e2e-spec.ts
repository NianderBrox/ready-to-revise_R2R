import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { createTestApp } from './utils/create-test-app';
describe('Health API (e2e)', () => {
    let app: INestApplication;
    beforeAll(async () => {
        app = await createTestApp();
    });
    afterAll(async () => {
        await app.close();
    });
    it('GET /api/v1/health should return healthy status', async () => {
        const response = await request(app.getHttpServer())
            .get('/api/v1/health')
            .expect(200);
        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');
        expect(response.body.data).toEqual(
            expect.objectContaining({ status: 'ok' }),
        );
        expect(response.body.data.timestamp).toEqual(expect.any(String));
    });
});
