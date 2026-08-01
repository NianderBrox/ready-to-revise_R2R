import { INestApplication } from '@nestjs/common';
import request from 'supertest';

import { createTestApp } from './utils/create-test-app';

describe('Chapters API (e2e)', () => {
    let app: INestApplication;

    beforeAll(async () => {
        app = await createTestApp();
    });

    afterAll(async () => {
        await app.close();
    });

    it('POST /api/v1/chapters should create a chapter under a subject', async () => {
        const subjectName = `Biology ${Date.now()}`;

        const subjectResponse = await request(app.getHttpServer())
            .post('/api/v1/subjects')
            .send({
                name: subjectName,
            })
            .expect(201);

        const subjectId = subjectResponse.body.data.id;

        const chapterName = `Cell Structure ${Date.now()}`;

        const response = await request(app.getHttpServer())
            .post('/api/v1/chapters')
            .send({
                name: `  ${chapterName}  `,
                subjectId,
            })
            .expect(201);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                id: expect.any(String),
                name: chapterName,
                subjectId,
                createdAt: expect.any(String),
                updatedAt: expect.any(String),
            }),
        );
    });

    it('POST /api/v1/chapters should reject a non-existent subject', async () => {
        const response = await request(app.getHttpServer())
            .post('/api/v1/chapters')
            .send({
                name: `Invalid Chapter ${Date.now()}`,
                subjectId: '00000000-0000-4000-8000-000000000000',
            })
            .expect(404);
        expect(response.body.success).toBe(false);
    });

    it('POST /api/v1/chapters should reject duplicate names within the same subject', async () => {
        const subjectName = `Mathematics ${Date.now()}`;

        const subjectResponse = await request(app.getHttpServer())
            .post('/api/v1/subjects')
            .send({
                name: subjectName,
            })
            .expect(201);

        const subjectId = subjectResponse.body.data.id;

        const chapterName = `Algebra ${Date.now()}`;

        await request(app.getHttpServer())
            .post('/api/v1/chapters')
            .send({
                name: chapterName,
                subjectId,
            })
            .expect(201);

        const response = await request(app.getHttpServer())
            .post('/api/v1/chapters')
            .send({
                name: `  ${chapterName.toLowerCase()}  `,
                subjectId,
            })
            .expect(409);

        expect(response.body.success).toBe(false);
    });

    it('GET /api/v1/chapters should return all chapters sorted by subject and name', async () => {
        const suffix = Date.now();

        const subjectResponse = await request(app.getHttpServer())
            .post('/api/v1/subjects')
            .send({
                name: `Physics ${suffix}`,
            })
            .expect(201);

        const subjectId = subjectResponse.body.data.id;

        await request(app.getHttpServer())
            .post('/api/v1/chapters')
            .send({
                name: `Zoology ${suffix}`,
                subjectId,
            })
            .expect(201);

        await request(app.getHttpServer())
            .post('/api/v1/chapters')
            .send({
                name: `Algebra ${suffix}`,
                subjectId,
            })
            .expect(201);

        const response = await request(app.getHttpServer())
            .get('/api/v1/chapters')
            .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');
        expect(Array.isArray(response.body.data)).toBe(true);

        const chapters = response.body.data.filter(
            (chapter: { subjectId: string }) => chapter.subjectId === subjectId,
        );

        expect(chapters).toHaveLength(2);
        expect(chapters[0].name).toBe(`Algebra ${suffix}`);
        expect(chapters[1].name).toBe(`Zoology ${suffix}`);
    });

    it('GET /api/v1/chapters/:id should return a chapter', async () => {
        const suffix = Date.now();

        const subjectResponse = await request(app.getHttpServer())
            .post('/api/v1/subjects')
            .send({
                name: `Chemistry ${suffix}`,
            })
            .expect(201);

        const subjectId = subjectResponse.body.data.id;

        const chapterResponse = await request(app.getHttpServer())
            .post('/api/v1/chapters')
            .send({
                name: `Organic Chemistry ${suffix}`,
                subjectId,
            })
            .expect(201);

        const chapterId = chapterResponse.body.data.id;

        const response = await request(app.getHttpServer())
            .get(`/api/v1/chapters/${chapterId}`)
            .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.data).toEqual(
            expect.objectContaining({
                id: chapterId,
                name: `Organic Chemistry ${suffix}`,
                subjectId,
            }),
        );
    });

    it('PATCH /api/v1/chapters/:id should update a chapter', async () => {
        const suffix = Date.now();

        const subjectResponse = await request(app.getHttpServer())
            .post('/api/v1/subjects')
            .send({
                name: `Computer Science ${suffix}`,
            })
            .expect(201);

        const subjectId = subjectResponse.body.data.id;

        const chapterResponse = await request(app.getHttpServer())
            .post('/api/v1/chapters')
            .send({
                name: `Networks ${suffix}`,
                subjectId,
            })
            .expect(201);

        const chapterId = chapterResponse.body.data.id;

        const response = await request(app.getHttpServer())
            .patch(`/api/v1/chapters/${chapterId}`)
            .send({
                name: `Computer Networks ${suffix}`,
            })
            .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.data).toEqual(
            expect.objectContaining({
                id: chapterId,
                name: `Computer Networks ${suffix}`,
                subjectId,
            }),
        );
    });

    it('DELETE /api/v1/chapters/:id should delete a chapter', async () => {
        const suffix = Date.now();

        const subjectResponse = await request(app.getHttpServer())
            .post('/api/v1/subjects')
            .send({
                name: `Biochemistry ${suffix}`,
            })
            .expect(201);

        const subjectId = subjectResponse.body.data.id;

        const chapterResponse = await request(app.getHttpServer())
            .post('/api/v1/chapters')
            .send({
                name: `Enzymes ${suffix}`,
                subjectId,
            })
            .expect(201);

        const chapterId = chapterResponse.body.data.id;

        await request(app.getHttpServer())
            .delete(`/api/v1/chapters/${chapterId}`)
            .expect(200);

        await request(app.getHttpServer())
            .get(`/api/v1/chapters/${chapterId}`)
            .expect(404);
    });
});
