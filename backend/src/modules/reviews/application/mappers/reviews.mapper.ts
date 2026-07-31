import { Review } from '@prisma/client';
import { ReviewResponseDto } from '../../presentation/dto/review-response.dto';

export class ReviewsMapper {
    static toResponse(review: Review): ReviewResponseDto {
        return {
            id: review.id,
            studyItemId: review.studyItemId,
            result: review.result,
            intervalDays: review.intervalDays,
            reviewedAt: review.reviewedAt,
            nextReviewAt: review.nextReviewAt,
            createdAt: review.createdAt,
            updatedAt: review.updatedAt,
        };
    }
}
