import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { createTestApp } from './utils/create-test-app';

describe('Study Items API (e2e)', () => {
    let app: INestApplication;
    let accessToken: string;

    beforeAll(async () => {
        app = await createTestApp();

        const email = `study-items-${Date.now()}@example.com`;

        const response = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Study Items Test User',
                email,
                password: 'TestPassword123!',
            })
            .expect(201);

        accessToken = response.body.data.accessToken;
    });

    afterAll(async () => {
        await app.close();
    });

    it('POST /api/v1/study-items should create a study item for the authenticated user', async () => {
        const response = await request(app.getHttpServer())
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

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                id: expect.any(String),
                type: 'QUESTION',
                title: 'What is TCP?',
                content:
                    'TCP is a connection-oriented transport layer protocol.',
                difficulty: 'MEDIUM',
                createdAt: expect.any(String),
                updatedAt: expect.any(String),
            }),
        );
    });

    it('should prevent another user from accessing a study item', async () => {
        const userAEmail = `study-item-owner-${Date.now()}@example.com`;
        const userBEmail = `study-item-other-${Date.now()}@example.com`;

        const userAResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Study Item Owner',
                email: userAEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        const userAToken = userAResponse.body.data.accessToken;

        const userBResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Other User',
                email: userBEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        const userBToken = userBResponse.body.data.accessToken;

        const createResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${userAToken}`)
            .send({
                type: 'QUESTION',
                title: 'Private Question',
                content: 'This belongs to User A.',
                difficulty: 'EASY',
            })
            .expect(201);

        const studyItemId = createResponse.body.data.id;

        await request(app.getHttpServer())
            .get(`/api/v1/study-items/${studyItemId}`)
            .set('Authorization', `Bearer ${userAToken}`)
            .expect(200);

        await request(app.getHttpServer())
            .get(`/api/v1/study-items/${studyItemId}`)
            .set('Authorization', `Bearer ${userBToken}`)
            .expect(404);

        await request(app.getHttpServer())
            .patch(`/api/v1/study-items/${studyItemId}`)
            .set('Authorization', `Bearer ${userBToken}`)
            .send({
                difficulty: 'HARD',
            })
            .expect(404);

        await request(app.getHttpServer())
            .delete(`/api/v1/study-items/${studyItemId}`)
            .set('Authorization', `Bearer ${userBToken}`)
            .expect(404);

        await request(app.getHttpServer())
            .get(`/api/v1/study-items/${studyItemId}`)
            .set('Authorization', `Bearer ${userAToken}`)
            .expect(200);
    });

    it('should reject unauthenticated requests', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .send({
                type: 'QUESTION',
                title: 'Unauthorized Question',
                content: 'This should not be created.',
            })
            .expect(401);

        await request(app.getHttpServer())
            .get('/api/v1/study-items')
            .expect(401);

        await request(app.getHttpServer())
            .get('/api/v1/study-items/00000000-0000-4000-8000-000000000000')
            .expect(401);

        await request(app.getHttpServer())
            .patch('/api/v1/study-items/00000000-0000-4000-8000-000000000000')
            .send({
                title: 'Unauthorized Update',
            })
            .expect(401);

        await request(app.getHttpServer())
            .delete('/api/v1/study-items/00000000-0000-4000-8000-000000000000')
            .expect(401);
    });

    it('should reject a study item when both title and content are empty', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'QUESTION',
                title: '   ',
                content: '   ',
            })
            .expect(400);
    });

    it('should reject a study item with a non-existent topicId', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'QUESTION',
                title: 'Question with invalid topic',
                content: 'This topic does not exist.',
                topicId: '00000000-0000-4000-8000-000000000000',
            })
            .expect(404);
    });

    it('GET /api/v1/study-items should return only the authenticated user items', async () => {
        const userAEmail = `study-list-a-${Date.now()}@example.com`;
        const userBEmail = `study-list-b-${Date.now()}@example.com`;

        const userAResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Study List User A',
                email: userAEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        const userAToken = userAResponse.body.data.accessToken;

        const userBResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Study List User B',
                email: userBEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        const userBToken = userBResponse.body.data.accessToken;

        await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${userAToken}`)
            .send({
                type: 'QUESTION',
                title: 'User A Question',
                content: 'This belongs to User A.',
            })
            .expect(201);

        await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${userAToken}`)
            .send({
                type: 'NOTE',
                title: 'User A Note',
                content: 'Another item belonging to User A.',
            })
            .expect(201);

        await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${userBToken}`)
            .send({
                type: 'QUESTION',
                title: 'User B Question',
                content: 'This belongs to User B.',
            })
            .expect(201);

        const userAList = await request(app.getHttpServer())
            .get('/api/v1/study-items')
            .set('Authorization', `Bearer ${userAToken}`)
            .expect(200);

        expect(userAList.body.success).toBe(true);
        expect(userAList.body.message).toBe('Request successful');
        expect(Array.isArray(userAList.body.data)).toBe(true);

        expect(userAList.body.data).toHaveLength(2);

        expect(
            userAList.body.data.every((item: { title: string }) =>
                item.title.startsWith('User A'),
            ),
        ).toBe(true);

        expect(
            userAList.body.data.some(
                (item: { title: string }) => item.title === 'User B Question',
            ),
        ).toBe(false);
    });

    it('GET /api/v1/study-items/:id should return the authenticated user study item', async () => {
        const createResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'QUESTION',
                title: 'Specific Study Item',
                content: 'This item should be retrievable by its ID.',
                difficulty: 'MEDIUM',
            })
            .expect(201);

        const studyItemId = createResponse.body.data.id;

        const response = await request(app.getHttpServer())
            .get(`/api/v1/study-items/${studyItemId}`)
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                id: studyItemId,
                type: 'QUESTION',
                title: 'Specific Study Item',
                content: 'This item should be retrievable by its ID.',
                difficulty: 'MEDIUM',
            }),
        );
    });

    it('should reject attempts to modify title', async () => {
        const createResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                title: 'Original title',
                content: 'Original content',
                type: 'QUESTION',
            })
            .expect(201);

        const studyItemId = createResponse.body.data.id;

        const response = await request(app.getHttpServer())
            .patch(`/api/v1/study-items/${studyItemId}`)
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                title: 'Attempted title modification',
            })
            .expect(400);

        expect(response.body.success).toBe(false);
    });

    it('should reject attempts to modify content', async () => {
        const createResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                title: 'Original title',
                content: 'Original content',
                type: 'QUESTION',
            })
            .expect(201);

        const studyItemId = createResponse.body.data.id;

        const response = await request(app.getHttpServer())
            .patch(`/api/v1/study-items/${studyItemId}`)
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                content: 'Attempted content modification',
            })
            .expect(400);

        expect(response.body.success).toBe(false);
    });

    it('should reject attempts to modify type', async () => {
        const createResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                title: 'Original title',
                content: 'Original content',
                type: 'QUESTION',
            })
            .expect(201);

        const studyItemId = createResponse.body.data.id;

        const response = await request(app.getHttpServer())
            .patch(`/api/v1/study-items/${studyItemId}`)
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'QUESTION',
            })
            .expect(400);

        expect(response.body.success).toBe(false);
    });
});
