import { Module } from '@nestjs/common';

import { PrismaModule } from '../../prisma/prisma.module';

import { AttachmentsController } from './attachments.controller';
import { AttachmentsRepository } from './attachments.repository';
import { AttachmentsService } from './attachments.service';

@Module({
  imports: [
    PrismaModule,
  ],
  controllers: [
    AttachmentsController,
  ],
  providers: [
    AttachmentsRepository,
    AttachmentsService,
  ],
  exports: [
    AttachmentsRepository,
  ],
})
export class AttachmentsModule {}