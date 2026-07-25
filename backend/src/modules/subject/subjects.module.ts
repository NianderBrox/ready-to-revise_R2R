import { Module } from '@nestjs/common';

import { SubjectsController } from './subjects.controller';
import { SubjectsService } from './subjects.service';
import { SubjectsRepository } from './subjects.repository';

import { PrismaModule } from '../../prisma/prisma.module';

@Module({
  imports: [PrismaModule],
  controllers: [SubjectsController],
  providers: [
    SubjectsService,
    SubjectsRepository,
  ],
  exports: [SubjectsRepository],
})
export class SubjectsModule {}