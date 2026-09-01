import {
    BadRequestException,
    Injectable,
    NotFoundException,
} from '@nestjs/common';
import { CreateReviewDto } from '../../presentation/dto/create-review.dto';
import { SelfGradeReviewDto } from '../../presentation/dto/self-grade-review.dto';
import { ReviewScheduler } from '../../infrastructure/utils/review-scheduler';
import {
    ConfidenceInferenceService,
    ConfidenceLevel,
} from '../../domain/services/confidence-inference.service';
import { ReviewResponseDto } from '../../presentation/dto/review-response.dto';
import { ReviewsRepository } from '../../infrastructure/repositories/reviews.repository';
import { ReviewsMapper } from '../mappers/reviews.mapper';
import { ReviewResult } from '@prisma/client';

@Injectable()
export class ReviewsService {
    constructor(
        private readonly repository: ReviewsRepository,
        private readonly confidenceInference: ConfidenceInferenceService,
    ) {}

    async create(
        userId: string,
        dto: CreateReviewDto,
    ): Promise<ReviewResponseDto> {
        const studyItem = await this.repository.getStudyItemGradingInfo(
            dto.studyItemId,
            userId,
        );

        if (!studyItem) {
            throw new NotFoundException('Study item not found.');
        }

        if (studyItem.correctAnswerIndex === null) {
            throw new BadRequestException(
                'Study item is not gradable (missing MCQ answer key).',
            );
        }

        const isCorrect =
            dto.selectedOptionIndex === studyItem.correctAnswerIndex;

        const confidence = this.confidenceInference.infer({
            isCorrect,

            responseTimeMs: dto.responseTimeMs,

            hesitationMs: dto.hesitationMs,

            answerChanges: dto.answerChanges,
        });

        const result = this.mapToResult(isCorrect, confidence.level);

        const latest = await this.repository.findLatest(dto.studyItemId);

        const intervalDays = ReviewScheduler.calculate(
            result,
            latest?.intervalDays ?? null,
        );

        const nextReviewAt = ReviewScheduler.nextReviewDate(intervalDays);

        const review = await this.repository.create({
            studyItemId: dto.studyItemId,

            result,

            intervalDays,

            nextReviewAt,

            selectedOptionIndex: dto.selectedOptionIndex,

            isCorrect,

            confidenceScore: confidence.score,

            responseTimeMs: dto.responseTimeMs ?? null,

            hesitationMs: dto.hesitationMs ?? null,

            answerChanges: dto.answerChanges ?? null,

            sessionId: dto.sessionId ?? null,
        });

        await this.repository.updateItemNextReviewAt(
            dto.studyItemId,
            nextReviewAt,
        );

        return ReviewsMapper.toResponse(review);
    }

    async selfGrade(
        userId: string,
        dto: SelfGradeReviewDto,
    ): Promise<ReviewResponseDto> {
        const exists = await this.repository.studyItemExists(
            dto.studyItemId,
            userId,
        );

        if (!exists) {
            throw new NotFoundException(
                'Study item not found or access denied.',
            );
        }

        const latest = await this.repository.findLatest(dto.studyItemId);

        const intervalDays = ReviewScheduler.calculate(
            ReviewResult.MEMORIZED,
            latest?.intervalDays ?? null,
        );

        const nextReviewAt = ReviewScheduler.nextReviewDate(intervalDays);

        const review = await this.repository.create({
            studyItemId: dto.studyItemId,

            result: ReviewResult.MEMORIZED,

            intervalDays,

            nextReviewAt,

            selectedOptionIndex: null,

            isCorrect: null,

            confidenceScore: null,

            responseTimeMs: dto.responseTimeMs ?? null,

            hesitationMs: null,

            answerChanges: null,

            sessionId: dto.sessionId ?? null,
        });

        await this.repository.updateItemNextReviewAt(
            dto.studyItemId,
            nextReviewAt,
        );

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

        return reviews.map((review) => ReviewsMapper.toResponse(review));
    }

    private mapToResult(
        isCorrect: boolean,
        level: ConfidenceLevel,
    ): ReviewResult {
        if (!isCorrect) {
            return ReviewResult.AGAIN;
        }

        switch (level) {
            case ConfidenceLevel.HIGH:
                return ReviewResult.EASY;

            case ConfidenceLevel.MEDIUM:
                return ReviewResult.GOOD;

            default:
                return ReviewResult.HARD;
        }
    }
}
