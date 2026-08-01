import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { createTestApp } from './utils/create-test-app';

describe('Dashboard API (e2e)', () => {
    let app: INestApplication;
    let accessToken: string;

    beforeAll(async () => {
        app = await createTestApp();

        const email = `dashboard-${Date.now()}@example.com`;

        const response = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Dashboard Test User',
                email,
                password: 'TestPassword123!',
            })
            .expect(201);

        accessToken = response.body.data.accessToken;
    });

    afterAll(async () => {
        await app.close();
    });

    it('GET /api/v1/dashboard should return dashboard data for the authenticated user', async () => {
        const response = await request(app.getHttpServer())
            .get('/api/v1/dashboard')
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                user: {
                    name: 'Dashboard Test User',
                },

                stats: expect.objectContaining({
                    studyItems: expect.any(Number),
                    subjects: expect.any(Number),
                    chapters: expect.any(Number),
                    topics: expect.any(Number),
                    inboxItems: expect.any(Number),
                }),

                reviews: expect.objectContaining({
                    dueToday: expect.any(Number),
                    upcoming: expect.any(Number),
                    completedToday: expect.any(Number),
                }),

                progress: expect.objectContaining({
                    completionPercentage: expect.any(Number),
                    streakDays: expect.any(Number),
                }),

                recentActivity: expect.any(Array),

                ai: {
                    suggestion: null,
                },
            }),
        );
    });

    it('should return correct study item and inbox counts', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'QUESTION',
                title: 'Dashboard Question',
                content: 'Question for dashboard testing.',
                difficulty: 'MEDIUM',
            })
            .expect(201);

        await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'NOTE',
                title: 'Dashboard Note',
                content: 'Note for dashboard testing.',
            })
            .expect(201);

        const response = await request(app.getHttpServer())
            .get('/api/v1/dashboard')
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(200);

        expect(response.body.data.stats.studyItems).toBe(2);
        expect(response.body.data.stats.inboxItems).toBe(2);
    });

    it('should reject unauthenticated requests', async () => {
        await request(app.getHttpServer()).get('/api/v1/dashboard').expect(401);
    });

    it('should only return dashboard data for the authenticated user', async () => {
        const otherUserEmail = `dashboard-other-${Date.now()}@example.com`;

        const otherUserResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Other Dashboard User',
                email: otherUserEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        const otherUserToken = otherUserResponse.body.data.accessToken;

        await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${otherUserToken}`)
            .send({
                type: 'QUESTION',
                title: 'Other User Question',
                content: 'This belongs to another user.',
                difficulty: 'EASY',
            })
            .expect(201);

        const response = await request(app.getHttpServer())
            .get('/api/v1/dashboard')
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(200);

        expect(response.body.data.stats.studyItems).toBe(2);
        expect(response.body.data.stats.inboxItems).toBe(2);
    });
});
