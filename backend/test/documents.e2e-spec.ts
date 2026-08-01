import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { createTestApp } from './utils/create-test-app';
import { FakeDocumentAnalysisProvider } from './utils/fake-document-analysis.provider';

describe('Documents API (e2e)', () => {
    let app: INestApplication;

    let userAToken: string;
    let userBToken: string;

    let userADocumentId: string;

    beforeAll(async () => {
        app = await createTestApp({
            documentAnalysisProvider: new FakeDocumentAnalysisProvider(),
        });

        const userAEmail = `documents-a-${Date.now()}@example.com`;
        const userBEmail = `documents-b-${Date.now()}@example.com`;

        const userAResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Documents User A',
                email: userAEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        userAToken = userAResponse.body.data.accessToken;

        const userBResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Documents User B',
                email: userBEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        userBToken = userBResponse.body.data.accessToken;
    });

    afterAll(async () => {
        await app.close();
    });

    it('should upload a document', async () => {
        const response = await request(app.getHttpServer())
            .post('/api/v1/documents')
            .set('Authorization', `Bearer ${userAToken}`)
            .attach(
                'file',
                Buffer.from('%PDF-1.4\nTest PDF content'),
                'biology.pdf',
            )
            .expect(201);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                documentId: expect.any(String),
                status: 'READY',
            }),
        );

        expect(response.body.data.analysis).toEqual(
            expect.objectContaining({
                title: 'Test Document',
                subject: 'Biology',
                chapter: 'Cell Biology',
                topic: 'Cell Structure',
            }),
        );

        userADocumentId = response.body.data.documentId;
    });

    it('should list only the authenticated user documents', async () => {
        const response = await request(app.getHttpServer())
            .get('/api/v1/documents')
            .set('Authorization', `Bearer ${userAToken}`)
            .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');
        expect(Array.isArray(response.body.data)).toBe(true);

        expect(response.body.data).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    id: userADocumentId,
                }),
            ]),
        );
    });

    it('should return document details for the owner', async () => {
        const response = await request(app.getHttpServer())
            .get(`/api/v1/documents/${userADocumentId}`)
            .set('Authorization', `Bearer ${userAToken}`)
            .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                id: userADocumentId,
                originalName: 'biology.pdf',
            }),
        );
    });

    it('should prevent another user from accessing the document', async () => {
        await request(app.getHttpServer())
            .get(`/api/v1/documents/${userADocumentId}`)
            .set('Authorization', `Bearer ${userBToken}`)
            .expect(404);
    });

    it('should download the document for the owner', async () => {
        const response = await request(app.getHttpServer())
            .get(`/api/v1/documents/${userADocumentId}/file`)
            .set('Authorization', `Bearer ${userAToken}`)
            .expect(200);

        expect(response.headers['content-type']).toContain('application/pdf');

        expect(response.headers['content-disposition']).toContain(
            'biology.pdf',
        );

        expect(response.body.toString()).toContain('%PDF-1.4');
    });

    it('should prevent another user from downloading the document', async () => {
        await request(app.getHttpServer())
            .get(`/api/v1/documents/${userADocumentId}/file`)
            .set('Authorization', `Bearer ${userBToken}`)
            .expect(404);
    });

    it('should reject unauthenticated document listing', async () => {
        await request(app.getHttpServer()).get('/api/v1/documents').expect(401);
    });

    it('should reject unauthenticated document access', async () => {
        await request(app.getHttpServer())
            .get(`/api/v1/documents/${userADocumentId}`)
            .expect(401);
    });

    it('should reject an invalid file upload', async () => {
        await request(app.getHttpServer())
            .post('/api/v1/documents')
            .set('Authorization', `Bearer ${userAToken}`)
            .attach(
                'file',
                Buffer.from('invalid executable content'),
                'malware.exe',
            )
            .expect(400);
    });
});
