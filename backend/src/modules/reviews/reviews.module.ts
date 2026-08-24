import { Module } from '@nestjs/common';
import { PrismaModule } from '../../prisma/prisma.module';
import { ReviewsService } from './application/services/reviews.service';
import { ReviewsRepository } from './infrastructure/repositories/reviews.repository';
import { ReviewsController } from './presentation/controllers/reviews.controller';
import { ConfidenceInferenceService } from './domain/services/confidence-inference.service';

@Module({
    imports: [PrismaModule],
    controllers: [ReviewsController],
    providers: [ReviewsRepository, ReviewsService, ConfidenceInferenceService],
    exports: [ReviewsRepository],
})
export class ReviewsModule {}
