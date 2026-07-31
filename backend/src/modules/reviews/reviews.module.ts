import { Module } from '@nestjs/common';
import { PrismaModule } from '../../prisma/prisma.module';
import { ReviewsService } from './application/services/reviews.service';
import { ReviewsRepository } from './domain/repositories/reviews.repository';
import { ReviewsController } from './presentation/controllers/reviews.controller';

@Module({
    imports: [PrismaModule],
    controllers: [ReviewsController],
    providers: [ReviewsRepository, ReviewsService],
    exports: [ReviewsRepository],
})
export class ReviewsModule {}
