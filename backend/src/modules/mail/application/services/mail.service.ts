import {
    Injectable,
    Logger,
    OnModuleInit,
    ServiceUnavailableException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as nodemailer from 'nodemailer';

@Injectable()
export class MailService implements OnModuleInit {
    private readonly logger = new Logger(MailService.name);
    private readonly transporter: nodemailer.Transporter | null;
    private readonly from: string;

    constructor(private readonly config: ConfigService) {
        const host = config.get<string>('SMTP_HOST')?.trim() || undefined;
        const rawPort = config.get<string | number>('SMTP_PORT');
        const parsedPort = Number(rawPort);
        const port =
            Number.isFinite(parsedPort) && parsedPort > 0 ? parsedPort : 587;
        const user = config.get<string>('SMTP_USER')?.trim() || undefined;
        // Gmail App Passwords are shown as "xxxx xxxx xxxx xxxx" — spaces must be stripped.
        const pass =
            config.get<string>('SMTP_PASS')?.replace(/\s/g, '') || undefined;
        this.from =
            config.get<string>('SMTP_FROM')?.trim() ||
            'Ready to Revise <no-reply@readytorevise.app>';

        this.transporter = host
            ? nodemailer.createTransport({
                  host,
                  port,
                  secure: port === 465,
                  requireTLS: port === 587,
                  auth: user ? { user, pass } : undefined,
                  connectionTimeout: 10_000,
                  greetingTimeout: 10_000,
                  socketTimeout: 10_000,
              })
            : null;

        if (!this.transporter) {
            this.logger.warn(
                'SMTP_HOST is not set. OTP emails will only be logged, not sent.',
            );
        }
    }

    async onModuleInit(): Promise<void> {
        if (!this.transporter) {
            return;
        }
        try {
            await this.transporter.verify();
            this.logger.log(
                'SMTP transporter verified. OTP emails will be sent.',
            );
        } catch (error) {
            this.logger.error(
                `SMTP verify failed. Check SMTP_HOST/PORT/USER/PASS. ${(error as Error)?.message ?? error}`,
            );
        }
    }

    async sendOtpEmail(to: string, otp: string): Promise<void> {
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
            this.logger.error(
                `Failed to send OTP email to ${to}. ${(error as Error)?.message ?? error}`,
            );
            throw new ServiceUnavailableException(
                'Email service is temporarily unavailable. Please try again.',
            );
        }
    }
}
