import {
    Injectable,
    Logger,
    OnModuleInit,
    ServiceUnavailableException,
} from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { AxiosError } from 'axios';
import { lookup } from 'node:dns/promises';
import { isIP } from 'node:net';
import * as nodemailer from 'nodemailer';
import { firstValueFrom } from 'rxjs';

const BREVO_SEND_URL = 'https://api.brevo.com/v3/smtp/email';

@Injectable()
export class MailService implements OnModuleInit {
    private readonly logger = new Logger(MailService.name);
    private transporter: nodemailer.Transporter | null = null;
    private readonly from: string;
    private readonly host?: string;
    private readonly port: number;
    private readonly user?: string;
    private readonly pass?: string;
    private readonly brevoApiKey?: string;
    private readonly brevoSenderEmail?: string;
    private readonly brevoSenderName: string;

    constructor(
        private readonly config: ConfigService,
        private readonly http: HttpService,
    ) {
        this.host = config.get<string>('SMTP_HOST')?.trim() || undefined;
        const rawPort = config.get<string | number>('SMTP_PORT');
        const parsedPort = Number(rawPort);
        this.port =
            Number.isFinite(parsedPort) && parsedPort > 0 ? parsedPort : 587;
        this.user = config.get<string>('SMTP_USER')?.trim() || undefined;
        this.pass =
            config.get<string>('SMTP_PASS')?.replace(/\s/g, '') || undefined;
        this.from =
            config.get<string>('SMTP_FROM')?.trim() ||
            'Ready to Revise <no-reply@readytorevise.app>';
        this.brevoApiKey =
            config.get<string>('BREVO_API_KEY')?.trim() || undefined;
        this.brevoSenderEmail =
            config.get<string>('BREVO_SENDER_EMAIL')?.trim() || undefined;
        this.brevoSenderName =
            config.get<string>('BREVO_SENDER_NAME')?.trim() ||
            'Ready to Revise';

        if (!this.useBrevo && !this.host) {
            this.logger.warn(
                'SMTP_HOST is not set. OTP emails will only be logged, not sent.',
            );
        }
    }

    private get useBrevo(): boolean {
        return !!(this.brevoApiKey && this.brevoSenderEmail);
    }

    onModuleInit(): void {
        if (this.useBrevo) {
            this.logger.log(
                'Brevo HTTP email enabled. OTP emails will be sent via Brevo.',
            );
            return;
        }
        if (!this.host) {
            return;
        }
        void this.initialize();
    }

    async sendOtpEmail(to: string, otp: string): Promise<void> {
        if (this.useBrevo) {
            await this.sendViaBrevo(to, otp);
            return;
        }

        if (!this.transporter) {
            this.logger.warn(`SMTP not configured. OTP for ${to}: ${otp}`);
            return;
        }

        try {
            const info = await this.transporter.sendMail({
                from: this.from,
                to,
                subject: 'Your password reset code',
                text: `Your Ready to Revise password reset code is ${otp}. It expires in 5 minutes. If you did not request this, ignore this email.`,
                html: `<p>Your Ready to Revise password reset code is <b>${otp}</b>.</p><p>It expires in 5 minutes. If you did not request this, ignore this email.</p>`,
            });
            this.logger.log(
                `OTP email sent to ${to} (messageId: ${info.messageId})`,
            );
        } catch (error) {
            const err = error as { code?: string; message?: string };
            this.logger.error(
                `Failed to send OTP email to ${to} [${err?.code ?? 'UNKNOWN'}]. ${err?.message ?? error}`,
            );
            throw new ServiceUnavailableException(
                'Email service is temporarily unavailable. Please try again.',
            );
        }
    }

    private async sendViaBrevo(to: string, otp: string): Promise<void> {
        try {
            const response = await firstValueFrom(
                this.http.post(
                    BREVO_SEND_URL,
                    {
                        sender: {
                            name: this.brevoSenderName,
                            email: this.brevoSenderEmail,
                        },
                        to: [{ email: to }],
                        subject: 'Your password reset code',
                        textContent: `Your Ready to Revise password reset code is ${otp}. It expires in 5 minutes. If you did not request this, ignore this email.`,
                        htmlContent: `<p>Your Ready to Revise password reset code is <b>${otp}</b>.</p><p>It expires in 5 minutes. If you did not request this, ignore this email.</p>`,
                    },
                    {
                        headers: {
                            'api-key': this.brevoApiKey as string,
                            Accept: 'application/json',
                            'Content-Type': 'application/json',
                        },
                        timeout: 30_000,
                    },
                ),
            );
            this.logger.log(
                `OTP email sent via Brevo to ${to} (messageId: ${response.data?.messageId})`,
            );
        } catch (error) {
            const axiosError = error as AxiosError<{ message?: string }>;
            const status = axiosError?.response?.status;
            const detail =
                axiosError?.response?.data?.message ??
                (error as Error)?.message ??
                error;
            this.logger.error(
                `Failed to send OTP email via Brevo to ${to} [${status ?? 'UNKNOWN'}]. ${detail}`,
            );
            throw new ServiceUnavailableException(
                'Email service is temporarily unavailable. Please try again.',
            );
        }
    }

    private async initialize(): Promise<void> {
        try {
            let address = this.host as string;
            if (!isIP(address)) {
                try {
                    ({ address } = await lookup(address, { family: 4 }));
                } catch (error) {
                    const err = error as { code?: string; message?: string };
                    this.logger.error(
                        `SMTP DNS lookup failed for ${address} [${err?.code ?? 'UNKNOWN'}]. ${err?.message ?? error}`,
                    );
                    return;
                }
            }

            this.transporter = nodemailer.createTransport({
                host: address,
                servername: isIP(this.host as string) ? undefined : this.host,
                port: this.port,
                secure: this.port === 465,
                requireTLS: this.port === 587,
                auth: this.user
                    ? { user: this.user, pass: this.pass }
                    : undefined,
                connectionTimeout: 60_000,
                greetingTimeout: 30_000,
                socketTimeout: 60_000,
            });

            await this.verifyWithRetries(3);
        } catch (error) {
            const err = error as { code?: string; message?: string };
            this.logger.error(
                `SMTP initialization failed [${err?.code ?? 'UNKNOWN'}]. ${err?.message ?? error}`,
            );
        }
    }

    private async verifyWithRetries(attempts: number): Promise<void> {
        for (let attempt = 1; attempt <= attempts; attempt++) {
            try {
                await this.transporter?.verify();
                this.logger.log(
                    'SMTP transporter verified. OTP emails will be sent.',
                );
                return;
            } catch (error) {
                const err = error as { code?: string; message?: string };
                if (attempt === attempts) {
                    this.logger.error(
                        `SMTP verify failed [${err?.code ?? 'UNKNOWN'}] after ${attempts} attempts. Check SMTP_HOST/PORT/USER/PASS. ${err?.message ?? error}`,
                    );
                } else {
                    this.logger.warn(
                        `SMTP verify attempt ${attempt}/${attempts} failed [${err?.code ?? 'UNKNOWN'}], retrying. ${err?.message ?? error}`,
                    );
                    await new Promise((resolve) => setTimeout(resolve, 10_000));
                }
            }
        }
    }
}
