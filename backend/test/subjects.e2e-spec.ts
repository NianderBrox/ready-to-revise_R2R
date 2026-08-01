import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { createTestApp } from './utils/create-test-app';

describe('Subjects API (e2e)', () => {
    let app: INestApplication;

    beforeAll(async () => {
        app = await createTestApp();
    });

    afterAll(async () => {
        await app.close();
    });

    it('POST /api/v1/subjects should create a subject', async () => {
        const subjectName = `Cell Biology ${Date.now()}`;

        const response = await request(app.getHttpServer())
            .post('/api/v1/subjects')
            .send({
                name: `  ${subjectName}  `,
            })
            .expect(201);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                id: expect.any(String),
                name: subjectName,
                createdAt: expect.any(String),
                updatedAt: expect.any(String),
            }),
        );
    });

    it('POST /api/v1/subjects should reject case-insensitive duplicates', async () => {
        const subjectName = `Physics ${Date.now()}`;
        await request(app.getHttpServer())
            .post('/api/v1/subjects')
            .send({ name: subjectName })
            .expect(201);
        const response = await request(app.getHttpServer())
            .post('/api/v1/subjects')
            .send({ name: ` ${subjectName.toLowerCase()} ` })
            .expect(409);
        expect(response.body.success).toBe(false);
        expect(response.body.errors).toEqual([]);
    });
});
