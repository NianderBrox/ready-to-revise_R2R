import { Global, Module } from '@nestjs/common';
import { HttpModule } from '@nestjs/axios';
import { MailService } from './application/services/mail.service';

@Global()
@Module({
    imports: [HttpModule],
    providers: [MailService],
    exports: [MailService],
})
export class MailModule {}
