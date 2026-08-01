import { INestApplication } from '@nestjs/common';
import request from 'supertest';
import { createTestApp } from './utils/create-test-app';

describe('Auth API (e2e)', () => {
    let app: INestApplication;

    let email: string;
    let password: string;
    let accessToken: string;

    beforeAll(async () => {
        app = await createTestApp();

        email = `auth-${Date.now()}@example.com`;
        password = 'TestPassword123!';
    });

    afterAll(async () => {
        await app.close();
    });

    it('should register a new user', async () => {
        const response = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Auth Test User',
                email,
                password,
            })
            .expect(201);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                accessToken: expect.any(String),
            }),
        );

        accessToken = response.body.data.accessToken;
    });

    it('should reject duplicate email registration', async () => {
        const response = await request(app.getHttpServer())
            .post('/api/v1/auth/register')
            .send({
                name: 'Another User',
                email: email.toUpperCase(),
                password,
            })
            .expect(400);

        expect(response.body.success).toBe(false);
        expect(response.body.message).toBe('Email already registered');
    });

    it('should login with valid credentials', async () => {
        const response = await request(app.getHttpServer())
            .post('/api/v1/auth/login')
            .send({
                email: email.toUpperCase(),
                password,
            })
            .expect(201);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                accessToken: expect.any(String),
            }),
        );

        accessToken = response.body.data.accessToken;
    });

    it('should reject login with invalid password', async () => {
        const response = await request(app.getHttpServer())
            .post('/api/v1/auth/login')
            .send({
                email,
                password: 'WrongPassword123!',
            })
            .expect(401);

        expect(response.body.success).toBe(false);
        expect(response.body.message).toBe('Invalid email or password');
    });

    it('should return the authenticated user profile', async () => {
        const response = await request(app.getHttpServer())
            .get('/api/v1/auth/profile')
            .set('Authorization', `Bearer ${accessToken}`)
            .expect(200);

        expect(response.body.success).toBe(true);
        expect(response.body.message).toBe('Request successful');

        expect(response.body.data).toEqual(
            expect.objectContaining({
                userId: expect.any(String),
                email,
            }),
        );
    });

    it('should reject profile access without authentication', async () => {
        const response = await request(app.getHttpServer())
            .get('/api/v1/auth/profile')
            .expect(401);

        expect(response.body.success).toBe(false);
    });
});
