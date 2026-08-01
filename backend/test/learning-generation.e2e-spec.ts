import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { createTestApp } from './utils/create-test-app';

import { DOCUMENT_ANALYSIS_PROVIDER } from '../src/modules/document-analysis/infrastructure/tokens/document-analysis-provider.token';
import type { DocumentAnalysisProvider } from '../src/modules/document-analysis/domain/interfaces/document-analysis.provider';
import { DocumentAnalysisResult } from '../src/modules/document-analysis/domain/models/document-analysis.result';

import { QUESTION_GENERATION_PROVIDER } from '../src/modules/learning-generation/infrastructure/tokens/question-generation-provider.token';
import type { QuestionGenerationProvider } from '../src/modules/learning-generation/domain/interfaces/question-generation.provider';
import { QuestionGenerationResult } from '../src/modules/learning-generation/domain/models/question-generation.result';
import { GeneratedQuestion } from '../src/modules/learning-generation/domain/models/generated-questions';

import { Difficulty, DocumentStatus } from '@prisma/client';
import { DocumentsService } from '../src/modules/documents/application/services/documents.service';

describe('Learning Generation API (e2e)', () => {
    let app: INestApplication;
    let accessToken: string;

    let documentAnalysisProvider: DocumentAnalysisProvider;
    let questionGenerationProvider: QuestionGenerationProvider;

    beforeAll(async () => {
        app = await createTestApp();

        documentAnalysisProvider = app.get<DocumentAnalysisProvider>(
            DOCUMENT_ANALYSIS_PROVIDER,
        );

        questionGenerationProvider = app.get<QuestionGenerationProvider>(
            QUESTION_GENERATION_PROVIDER,
        );

        const email = `learning-generation-${Date.now()}@example.com`;

        const response = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Learning Generation Test User',
                email,
                password: 'TestPassword123!',
            })
            .expect(201);

        accessToken = response.body.data.accessToken;
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    afterAll(async () => {
        await app.close();
    });

    it('POST /api/v1/documents/:documentId/questions should generate and persist study items', async () => {
        jest.spyOn(documentAnalysisProvider, 'analyze').mockResolvedValue(
            new DocumentAnalysisResult(
                'TCP is a connection-oriented transport layer protocol.',
                'TCP Fundamentals',
                'Introduction to TCP.',
                'Computer Networks',
                'Transport Layer',
                'TCP',
                Difficulty.MEDIUM,
                ['TCP', 'transport layer', 'networking'],
            ),
        );

        const uploadResponse = await request(app.getHttpServer())
            .post('/api/v1/documents')
            .set('Authorization', `Bearer ${accessToken}`)
            .attach('file', Buffer.from('fake pdf content'), {
                filename: 'tcp.pdf',
                contentType: 'application/pdf',
            })
            .expect(201);

        expect(uploadResponse.body.success).toBe(true);

        const documentId = uploadResponse.body.data.documentId;

        expect(documentId).toEqual(expect.any(String));
        expect(uploadResponse.body.data.status).toBe('READY');

        jest.spyOn(questionGenerationProvider, 'generate').mockResolvedValue(
            Object.assign(new QuestionGenerationResult(), {
                questions: [
                    Object.assign(new GeneratedQuestion(), {
                        question: 'What is TCP?',
                        answer: 'TCP is a connection-oriented transport layer protocol.',
                        difficulty: Difficulty.MEDIUM,
                        explanation:
                            'TCP establishes a connection before transmitting data.',
                    }),
                    Object.assign(new GeneratedQuestion(), {
                        question: 'Which layer does TCP operate at?',
                        answer: 'TCP operates at the transport layer.',
                        difficulty: Difficulty.EASY,
                        explanation:
                            'TCP is one of the main protocols of the transport layer.',
                    }),
                ],
            }),
        );

        const response = await request(app.getHttpServer())
            .post(`/api/v1/documents/${documentId}/questions`)
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(201);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(questionGenerationProvider.generate).toHaveBeenCalledTimes(1);

        const studyItemsResponse = await request(app.getHttpServer())
            .get('/api/v1/study-items')
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(200);

        expect(studyItemsResponse.body.success).toBe(true);
        expect(Array.isArray(studyItemsResponse.body.data)).toBe(true);

        expect(studyItemsResponse.body.data).toHaveLength(2);

        expect(studyItemsResponse.body.data).toEqual(
            expect.arrayContaining([
                expect.objectContaining({
                    type: 'QUESTION',
                    title: 'What is TCP?',
                    content:
                        'TCP is a connection-oriented transport layer protocol.',
                    difficulty: 'MEDIUM',
                }),
                expect.objectContaining({
                    type: 'QUESTION',
                    title: 'Which layer does TCP operate at?',
                    content: 'TCP operates at the transport layer.',
                    difficulty: 'EASY',
                }),
            ]),
        );
    });

    it('should reject unauthenticated requests', async () => {
        await request(app.getHttpServer())
            .post(
                '/api/v1/documents/00000000-0000-4000-8000-000000000000/questions',
            )
            .expect(401);
    });

    it('should prevent another user from generating questions from a document they do not own', async () => {
        const userAEmail = `learning-owner-${Date.now()}@example.com`;
        const userBEmail = `learning-other-${Date.now()}@example.com`;

        const userAResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Learning Owner',
                email: userAEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        const userAToken = userAResponse.body.data.accessToken;

        const userBResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Learning Other User',
                email: userBEmail,
                password: 'TestPassword123!',
            })
            .expect(201);

        const userBToken = userBResponse.body.data.accessToken;

        jest.spyOn(documentAnalysisProvider, 'analyze').mockResolvedValue(
            new DocumentAnalysisResult(
                'TCP is a connection-oriented transport layer protocol.',
                'TCP Fundamentals',
                'Introduction to TCP.',
                'Computer Networks',
                'Transport Layer',
                'TCP',
                Difficulty.MEDIUM,
                ['TCP', 'transport layer', 'networking'],
            ),
        );

        const uploadResponse = await request(app.getHttpServer())
            .post('/api/v1/documents')
            .set('Authorization', `Bearer ${userAToken}`)
            .attach('file', Buffer.from('fake pdf content'), {
                filename: 'tcp.pdf',
                contentType: 'application/pdf',
            })
            .expect(201);

        const documentId = uploadResponse.body.data.documentId;

        await request(app.getHttpServer())
            .post(`/api/v1/documents/${documentId}/questions`)
            .set('Authorization', `Bearer ${userBToken}`)
            .expect(404);
    });

    it('should reject question generation for a document that is not READY', async () => {
        const email = `learning-not-ready-${Date.now()}@example.com`;

        const registerResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Learning Not Ready User',
                email,
                password: 'TestPassword123!',
            })
            .expect(201);

        const token = registerResponse.body.data.accessToken;

        const documentsService = app.get<DocumentsService>(DocumentsService);

        jest.spyOn(documentsService, 'getFileForUser').mockResolvedValueOnce({
            document: {
                id: '00000000-0000-4000-8000-000000000001',
                status: DocumentStatus.ANALYZING,
            } as any,
            file: {} as any,
        });

        await request(app.getHttpServer())
            .post(
                '/api/v1/documents/00000000-0000-4000-8000-000000000001/questions',
            )
            .set('Authorization', `Bearer ${token}`)
            .expect(400);
    });

    it('should return an error when question generation fails', async () => {
        const email = `learning-generation-failure-${Date.now()}@example.com`;

        const registerResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Learning Generation Failure User',
                email,
                password: 'TestPassword123!',
            })
            .expect(201);

        const token = registerResponse.body.data.accessToken;

        jest.spyOn(documentAnalysisProvider, 'analyze').mockResolvedValue(
            new DocumentAnalysisResult(
                'TCP is a connection-oriented transport layer protocol.',
                'TCP Fundamentals',
                'Introduction to TCP.',
                'Computer Networks',
                'Transport Layer',
                'TCP',
                Difficulty.MEDIUM,
                ['TCP', 'transport layer', 'networking'],
            ),
        );

        const uploadResponse = await request(app.getHttpServer())
            .post('/api/v1/documents')
            .set('Authorization', `Bearer ${token}`)
            .attach('file', Buffer.from('fake pdf content'), {
                filename: 'tcp.pdf',
                contentType: 'application/pdf',
            })
            .expect(201);

        const documentId = uploadResponse.body.data.documentId;

        expect(uploadResponse.body.data.status).toBe('READY');

        jest.spyOn(questionGenerationProvider, 'generate').mockRejectedValue(
            new Error('Question generation failed'),
        );

        const response = await request(app.getHttpServer())
            .post(`/api/v1/documents/${documentId}/questions`)
            .set('Authorization', `Bearer ${token}`)
            .expect(500);

        expect(response.body.success).toBe(false);

        const studyItemsResponse = await request(app.getHttpServer())
            .get('/api/v1/study-items')
            .set('Authorization', `Bearer ${token}`)
            .expect(200);

        expect(studyItemsResponse.body.data).toHaveLength(0);
    });

    it('should expose partial persistence if a later generated question fails', async () => {
        const email = `learning-partial-${Date.now()}@example.com`;

        const registerResponse = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Learning Partial Failure User',
                email,
                password: 'TestPassword123!',
            })
            .expect(201);

        const token = registerResponse.body.data.accessToken;

        jest.spyOn(documentAnalysisProvider, 'analyze').mockResolvedValue(
            new DocumentAnalysisResult(
                'TCP is a connection-oriented transport layer protocol.',
                'TCP Fundamentals',
                'Introduction to TCP.',
                'Computer Networks',
                'Transport Layer',
                'TCP',
                Difficulty.MEDIUM,
                ['TCP', 'transport layer', 'networking'],
            ),
        );

        const uploadResponse = await request(app.getHttpServer())
            .post('/api/v1/documents')
            .set('Authorization', `Bearer ${token}`)
            .attach('file', Buffer.from('fake pdf content'), {
                filename: 'tcp.pdf',
                contentType: 'application/pdf',
            })
            .expect(201);

        const documentId = uploadResponse.body.data.documentId;

        jest.spyOn(questionGenerationProvider, 'generate').mockResolvedValue(
            Object.assign(new QuestionGenerationResult(), {
                questions: [
                    Object.assign(new GeneratedQuestion(), {
                        question: 'What is TCP?',
                        answer: 'TCP is a connection-oriented transport layer protocol.',
                        difficulty: Difficulty.MEDIUM,
                        explanation: 'TCP establishes a connection.',
                    }),
                    Object.assign(new GeneratedQuestion(), {
                        question: 'This question should fail',
                        answer: 'This should not be persisted.',
                        difficulty: 'INVALID_DIFFICULTY' as any,
                    }),
                ],
            }),
        );

        await request(app.getHttpServer())
            .post(`/api/v1/documents/${documentId}/questions`)
            .set('Authorization', `Bearer ${token}`)
            .expect(500);

        const studyItemsResponse = await request(app.getHttpServer())
            .get('/api/v1/study-items')
            .set('Authorization', `Bearer ${token}`)
            .expect(200);

        expect(studyItemsResponse.body.success).toBe(true);
        expect(studyItemsResponse.body.data).toHaveLength(0);
    });
});
