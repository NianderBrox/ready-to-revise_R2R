import { HttpService } from '@nestjs/axios';
import { ServiceUnavailableException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { of, throwError } from 'rxjs';
import { MailService } from './mail.service';

const MANAGED_KEYS = [
    'SMTP_HOST',
    'SMTP_PORT',
    'SMTP_USER',
    'SMTP_PASS',
    'SMTP_FROM',
    'BREVO_API_KEY',
    'BREVO_SENDER_EMAIL',
    'BREVO_SENDER_NAME',
];

describe('MailService (Brevo)', () => {
    const savedEnv: Record<string, string | undefined> = {};

    beforeEach(() => {
        for (const key of MANAGED_KEYS) {
            savedEnv[key] = process.env[key];
            delete process.env[key];
        }
        jest.restoreAllMocks();
    });

    afterEach(() => {
        for (const key of MANAGED_KEYS) {
            if (savedEnv[key] === undefined) {
                delete process.env[key];
            } else {
                process.env[key] = savedEnv[key];
            }
        }
    });

    function buildService(postMock: jest.Mock) {
        const http = { post: postMock } as unknown as HttpService;
        return new MailService(new ConfigService(), http);
    }

    it('sends OTP via Brevo with api-key header and sender payload', async () => {
        const post = jest
            .fn()
            .mockReturnValue(of({ data: { messageId: 'abc123' } }));
        Object.assign(process.env, {
            BREVO_API_KEY: 'test-key',
            BREVO_SENDER_EMAIL: 'sender@gmail.com',
        });
        const service = buildService(post);

        await service.sendOtpEmail('user@gmail.com', '123456');

        expect(post).toHaveBeenCalledTimes(1);
        const [url, body, config] = post.mock.calls[0];
        expect(url).toBe('https://api.brevo.com/v3/smtp/email');
        expect(config.headers['api-key']).toBe('test-key');
        expect(body.sender).toEqual({
            name: 'Ready to Revise',
            email: 'sender@gmail.com',
        });
        expect(body.to).toEqual([{ email: 'user@gmail.com' }]);
        expect(body.subject).toBe('Your password reset code');
        expect(body.textContent).toContain('123456');
        expect(body.htmlContent).toContain('123456');
    });

    it('throws 503 when Brevo rejects the request', async () => {
        const failure = Object.assign(new Error('Unauthorized'), {
            response: { status: 401, data: { message: 'Unauthorized' } },
        });
        const post = jest.fn().mockReturnValue(throwError(() => failure));
        Object.assign(process.env, {
            BREVO_API_KEY: 'test-key',
            BREVO_SENDER_EMAIL: 'sender@gmail.com',
        });
        const service = buildService(post);

        await expect(
            service.sendOtpEmail('user@gmail.com', '123456'),
        ).rejects.toBeInstanceOf(ServiceUnavailableException);
    });

    it('falls back to log-only when neither Brevo nor SMTP is configured', async () => {
        const post = jest.fn();
        const service = buildService(post);

        await expect(
            service.sendOtpEmail('user@gmail.com', '123456'),
        ).resolves.toBeUndefined();
        expect(post).not.toHaveBeenCalled();
    });
});
