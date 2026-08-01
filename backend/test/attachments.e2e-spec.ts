import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { createTestApp } from './utils/create-test-app';

describe('Attachments API (e2e)', () => {
    let app: INestApplication;
    let accessToken: string;

    beforeAll(async () => {
        app = await createTestApp();

        const email = `attachments-${Date.now()}@example.com`;

        const response = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Attachments Test User',
                email,
                password: 'TestPassword123!',
            })
            .expect(201);

        accessToken = response.body.data.accessToken;
    });

    afterAll(async () => {
        await app.close();
    });

    it('POST /api/v1/attachments should create an attachment', async () => {
        const studyItemResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'QUESTION',
                title: 'Attachment Question',
                content: 'Question with an attachment.',
            })
            .expect(201);

        const studyItemId = studyItemResponse.body.data.id;

        const response = await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId,
                url: 'https://example.com/file.pdf',
                mimeType: 'application/pdf',
                fileSize: 1024,
                storageProvider: 'local',
            })
            .expect(201);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                id: expect.any(String),
                studyItemId,
                url: 'https://example.com/file.pdf',
                mimeType: 'application/pdf',
                fileSize: 1024,
                storageProvider: 'local',
                createdAt: expect.any(String),
                updatedAt: expect.any(String),
            }),
        );
    });

    it('GET /api/v1/attachments/:studyItemId should return attachments', async () => {
        const studyItemResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'NOTE',
                title: 'Attachment List Test',
                content: 'Testing attachment listing.',
            })
            .expect(201);

        const studyItemId = studyItemResponse.body.data.id;

        await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId,
                url: 'https://example.com/one.pdf',
                mimeType: 'application/pdf',
            })
            .expect(201);

        await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId,
                url: 'https://example.com/two.png',
                mimeType: 'image/png',
            })
            .expect(201);

        const response = await request(app.getHttpServer())
            .get(`/api/v1/attachments/${studyItemId}`)
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');
        expect(response.body.data).toHaveLength(2);

        expect(
            response.body.data.every(
                (attachment: { studyItemId: string }) =>
                    attachment.studyItemId === studyItemId,
            ),
        ).toBe(true);
    });

    it('DELETE /api/v1/attachments/:id should delete an attachment', async () => {
        const studyItemResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'NOTE',
                title: 'Attachment Delete Test',
                content: 'Testing attachment deletion.',
            })
            .expect(201);

        const studyItemId = studyItemResponse.body.data.id;

        const createResponse = await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId,
                url: 'https://example.com/delete.pdf',
                mimeType: 'application/pdf',
            })
            .expect(201);

        const attachmentId = createResponse.body.data.id;

        const deleteResponse = await request(app.getHttpServer())
            .delete(`/api/v1/attachments/${attachmentId}`)
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(200);

        expect(deleteResponse.body.success).toBe(true);
        expect(deleteResponse.body.message).toBe('Request successful');

        expect(deleteResponse.body.data).toEqual({
            success: true,
            message: 'Attachment deleted successfully.',
        });

        const response = await request(app.getHttpServer())
            .get(`/api/v1/attachments/${studyItemId}`)
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(200);

        expect(response.body.data).toHaveLength(0);
    });

    it('should reject unauthenticated requests', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .send({
                studyItemId: '00000000-0000-4000-8000-000000000000',
                url: 'https://example.com/file.pdf',
            })
            .expect(401);

        await request(app.getHttpServer())
            .get('/api/v1/attachments/00000000-0000-4000-8000-000000000000')
            .expect(401);

        await request(app.getHttpServer())
            .delete('/api/v1/attachments/00000000-0000-4000-8000-000000000000')
            .expect(401);
    });

    it('should reject attachments for another user study item', async () => {
        const otherUserEmail = `attachments-other-${Date.now()}@example.com`;

        const otherUserResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Other Attachment User',
                email: otherUserEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        const otherUserToken = otherUserResponse.body.data.accessToken;

        const studyItemResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${otherUserToken}`)
            .send({
                type: 'QUESTION',
                title: 'Private Study Item',
                content: 'Belongs to another user.',
            })
            .expect(201);

        const studyItemId = studyItemResponse.body.data.id;

        await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId,
                url: 'https://example.com/private.pdf',
            })
            .expect(404);

        await request(app.getHttpServer())
            .get(`/api/v1/attachments/${studyItemId}`)
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(404);
    });

    it('should reject an attachment for a non-existent study item', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId: '00000000-0000-4000-8000-000000000000',
                url: 'https://example.com/file.pdf',
            })
            .expect(404);
    });

    it('should reject invalid attachment data', async () => {
        const studyItemResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                type: 'NOTE',
                title: 'Validation Test',
                content: 'Testing attachment validation.',
            })
            .expect(201);

        const studyItemId = studyItemResponse.body.data.id;

        await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId,
                url: 'not-a-url',
            })
            .expect(400);

        await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId,
                url: 'https://example.com/file.pdf',
                fileSize: -1,
            })
            .expect(400);

        await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .set('Authorization', `Bearer ${accessToken}`)
            .send({
                studyItemId: 'not-a-uuid',
                url: 'https://example.com/file.pdf',
            })
            .expect(400);
    });

    it('should prevent another user from deleting an attachment', async () => {
        const ownerEmail = `attachment-owner-${Date.now()}@example.com`;
        const otherEmail = `attachment-delete-other-${Date.now()}@example.com`;

        const ownerResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Attachment Owner',
                email: ownerEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        const ownerToken = ownerResponse.body.data.accessToken;

        const otherResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Attachment Other User',
                email: otherEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        const otherToken = otherResponse.body.data.accessToken;

        const studyItemResponse = await request(app.getHttpServer())
            .post('/api/v1/study-items')
            .set('Authorization', `Bearer ${ownerToken}`)
            .send({
                type: 'NOTE',
                title: 'Owned Study Item',
                content: 'Private attachment test.',
            })
            .expect(201);

        const studyItemId = studyItemResponse.body.data.id;

        const attachmentResponse = await request(app.getHttpServer())
            .post('/api/v1/attachments')
            .set('Authorization', `Bearer ${ownerToken}`)
            .send({
                studyItemId,
                url: 'https://example.com/private.pdf',
            })
            .expect(201);

        const attachmentId = attachmentResponse.body.data.id;

        await request(app.getHttpServer())
            .delete(`/api/v1/attachments/${attachmentId}`)
            .set('Authorization', `Bearer ${otherToken}`)
            .expect(404);

        await request(app.getHttpServer())
            .get(`/api/v1/attachments/${studyItemId}`)
            .set('Authorization', `Bearer ${ownerToken}`)
            .expect(200);
    });
});
