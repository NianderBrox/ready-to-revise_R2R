import { Module } from '@nestjs/common';

import { PrismaModule } from '../../prisma/prisma.module';

import { ChaptersController } from './chapters.controller';
import { ChaptersService } from './chapters.service';
import { ChaptersRepository } from './chapters.repository';

@Module({
  imports: [
    PrismaModule,
  ],
  controllers: [
    ChaptersController,
  ],
  providers: [
    ChaptersService,
    ChaptersRepository,
  ],
  exports: [
    ChaptersRepository,
  ],
})
export class ChaptersModule {}