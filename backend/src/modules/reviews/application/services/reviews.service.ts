import { Injectable, NotFoundException } from '@nestjs/common';
import { CreateReviewDto } from '../../presentation/dto/create-review.dto';
import { ReviewScheduler } from '../../infrastructure/utils/review-scheduler';
import { ReviewResponseDto } from '../../presentation/dto/review-response.dto';
import { ReviewsRepository } from '../../infrastructure/repositories/reviews.repository';
import { ReviewsMapper } from '../mappers/reviews.mapper';

@Injectable()
export class ReviewsService {
    constructor(private readonly repository: ReviewsRepository) {}

    async create(
        userId: string,
        dto: CreateReviewDto,
    ): Promise<ReviewResponseDto> {
        const exists = await this.repository.studyItemExists(
            dto.studyItemId,
            userId,
        );

        if (!exists) {
            throw new NotFoundException('Study item not found.');
        }

        const intervalDays = ReviewScheduler.calculate(dto.result);

        const nextReviewAt = ReviewScheduler.nextReviewDate(intervalDays);

        const review = await this.repository.create({
            studyItemId: dto.studyItemId,
            result: dto.result,
            intervalDays,
            nextReviewAt,
        });

        return ReviewsMapper.toResponse(review);
    }

    async history(
        studyItemId: string,
        userId: string,
    ): Promise<ReviewResponseDto[]> {
        const exists = await this.repository.studyItemExists(
            studyItemId,
            userId,
        );

        if (!exists) {
            throw new NotFoundException(
                'Study item not found or access denied.',
            );
        }

        const reviews = await this.repository.findAllByStudyItem(studyItemId);

        return reviews.map(ReviewsMapper.toResponse);
    }
}
