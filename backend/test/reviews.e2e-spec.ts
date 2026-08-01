import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { createTestApp } from './utils/create-test-app';

describe('Reviews API (e2e)', () => {
    let app: INestApplication;
    let accessToken: string;
    let studyItemId: string;

    beforeAll(async () => {
        app = await createTestApp();

        const email = `reviews-${Date.now()}@example.com`;

        const registerResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Reviews Test User',
                email,
                password: 'TestPassword123!',
            })
            .expect(201);

        accessToken = registerResponse.body.data.accessToken;

        const studyItemResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'QUESTION',
                title: 'What is TCP?',
                content:
                    'TCP is a connection-oriented transport layer protocol.',
                difficulty: 'MEDIUM',
            })
            .expect(201);

        studyItemId = studyItemResponse.body.data.id;
    });

    afterAll(async () => {
        await app.close();
    });

    it('POST /api/v1/reviews should create a review and calculate the next review date', async () => {
        const beforeRequest = Date.now();

        const response = await request(app.getHttpServer())
            .post('/api/v1/reviews')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId,
                result: 'GOOD',
            })
            .expect(201);

        const afterRequest = Date.now();

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                id: expect.any(String),
                studyItemId,
                result: 'GOOD',
                intervalDays: 7,
                reviewedAt: expect.any(String),
                nextReviewAt: expect.any(String),
                createdAt: expect.any(String),
                updatedAt: expect.any(String),
            }),
        );

        const reviewedAt = new Date(response.body.data.reviewedAt).getTime();

        const nextReviewAt = new Date(
            response.body.data.nextReviewAt,
        ).getTime();

        expect(reviewedAt).toBeGreaterThanOrEqual(beforeRequest);
        expect(reviewedAt).toBeLessThanOrEqual(afterRequest);

        const expectedInterval = 7 * 24 * 60 * 60 * 1000;
        const actualInterval = nextReviewAt - reviewedAt;

        expect(actualInterval).toBeGreaterThanOrEqual(expectedInterval - 1000);

        expect(actualInterval).toBeLessThanOrEqual(expectedInterval + 1000);
    });

    it('GET /api/v1/reviews/study-items/:studyItemId should return review history', async () => {
        const response = await request(app.getHttpServer())
            .get(`/api/v1/reviews/study-items/${studyItemId}`)
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(Array.isArray(response.body.data)).toBe(true);
        expect(response.body.data).toHaveLength(1);

        expect(response.body.data[0]).toEqual(
            expect.objectContaining({
                studyItemId,
                result: 'GOOD',
                intervalDays: 7,
                reviewedAt: expect.any(String),
                nextReviewAt: expect.any(String),
            }),
        );
    });

    it('should reject unauthenticated requests', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/reviews')
            .send({
                studyItemId,
                result: 'GOOD',
            })
            .expect(401);

        await request(app.getHttpServer())
            .get(`/api/v1/reviews/study-items/${studyItemId}`)
            .expect(401);
    });

    it('should reject a review for a non-existent study item', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/reviews')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId: '00000000-0000-4000-8000-000000000000',
                result: 'GOOD',
            })
            .expect(404);
    });

    it('should reject invalid review results', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/reviews')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId,
                result: 'INVALID_RESULT',
            })
            .expect(400);
    });

    it('should reject an invalid study item id', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/reviews')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId: 'not-a-uuid',
                result: 'GOOD',
            })
            .expect(400);

        await request(app.getHttpServer())
            .get('/api/v1/reviews/study-items/not-a-uuid')
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(400);
    });
});
