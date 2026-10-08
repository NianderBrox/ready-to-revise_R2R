import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as nodemailer from 'nodemailer';

@Injectable()
export class MailService {
    private readonly logger = new Logger(MailService.name);
    private readonly transporter: nodemailer.Transporter | null;
    private readonly from: string;

    constructor(private readonly config: ConfigService) {
        const host = config.get<string>('SMTP_HOST');
        const port = config.get<number>('SMTP_PORT');
        const user = config.get<string>('SMTP_USER');
        const pass = config.get<string>('SMTP_PASS');
        this.from = config.get<string>('SMTP_FROM', 'Ready to Revise <no-reply@readytorevise.app>');

        this.transporter = host
            ? nodemailer.createTransport({
                  host,
                  port: port ?? 587,
                  secure: (port ?? 587) === 465,
                  auth: user ? { user, pass } : undefined,
              })
            : null;
    }

    async sendOtpEmail(to: string, otp: string): Promise<void> {
        if (!this.transporter) {
            this.logger.warn(`SMTP not configured. OTP for ${to}: ${otp}`);
            return;
        }

        await this.transporter.sendMail({
            from: this.from,
            to,
            subject: 'Your password reset code',
            text: `Your Ready to Revise password reset code is ${otp}. It expires in 5 minutes. If you did not request this, ignore this email.`,
            html: `<p>Your Ready to Revise password reset code is <b>${otp}</b>.</p><p>It expires in 5 minutes. If you did not request this, ignore this email.</p>`,
        });
    }
}
