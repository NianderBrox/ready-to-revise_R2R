import { Module } from '@nestjs/common';

import { PrismaModule } from '../../prisma/prisma.module';

import { ReviewsController } from './reviews.controller';
import { ReviewsRepository } from './reviews.repository';
import { ReviewsService } from './reviews.service';

@Module({
  imports: [
    PrismaModule,
  ],
  controllers: [
    ReviewsController,
  ],
  providers: [
    ReviewsRepository,
    ReviewsService,
  ],
  exports: [
    ReviewsRepository,
  ],
})
export class ReviewsModule {}