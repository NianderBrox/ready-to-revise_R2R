import { Module } from '@nestjs/common';
import { PrismaModule } from '../../prisma/prisma.module';
import { AttachmentsService } from './application/services/attachments.service';
import { AttachmentsRepository } from './infrastructure/repositories/attachments.repository';
import { AttachmentsController } from './presentation/controllers/attachments.controller';

@Module({
    imports: [PrismaModule],
    controllers: [AttachmentsController],
    providers: [AttachmentsRepository, AttachmentsService],
    exports: [AttachmentsRepository],
})
export class AttachmentsModule {}
