import {
    BadRequestException,
    Injectable,
    Logger,
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
import { MlHttpService } from '../../../ml-client/infrastructure/http/ml-http.service';
import { FeatureBuilderService } from '../../../recall-predictions/application/services/feature-builder.service';
import { RecallQueryRepository } from '../../../recall-predictions/infrastructure/repositories/recall-query.repository';
import { UserHistoryStats } from '../../../recall-predictions/domain/interfaces/recall-data.interfaces';

@Injectable()
export class ReviewsService {
    private readonly logger = new Logger(ReviewsService.name);

    constructor(
        private readonly repository: ReviewsRepository,
        private readonly confidenceInference: ConfidenceInferenceService,
        private readonly ml: MlHttpService,
        private readonly featureBuilder: FeatureBuilderService,
        private readonly recallRepository: RecallQueryRepository,
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

        const scheduled = await this.schedule(
            userId,
            dto.studyItemId,
            isCorrect,
            confidence.level,
            result,
            latest,
            {
                sessionDurationMinutes: dto.sessionDurationMinutes,
                questionPositionInSession: dto.questionPositionInSession,
            },
        );

        const intervalDays = scheduled.intervalDays;
        const nextReviewAt = scheduled.nextReviewAt;

        const review = await this.repository.create({
            studyItemId: dto.studyItemId,
            result,
            intervalDays,
            nextReviewAt,
            recallProbability: scheduled.fsrs.recallProbability,
            fsrsState: scheduled.fsrs.fsrsState,
            fsrsStep: scheduled.fsrs.fsrsStep,
            fsrsStability: scheduled.fsrs.fsrsStability,
            fsrsDifficulty: scheduled.fsrs.fsrsDifficulty,
            selectedOptionIndex: dto.selectedOptionIndex,
            isCorrect,
            confidenceScore: confidence.score,
            responseTimeMs: dto.responseTimeMs ?? null,
            hesitationMs: dto.hesitationMs ?? null,
            answerChanges: dto.answerChanges ?? null,
            sessionId: dto.sessionId ?? null,
            sessionDurationMinutes: dto.sessionDurationMinutes ?? null,
            questionPositionInSession: dto.questionPositionInSession ?? null,
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

        const scheduled = await this.schedule(
            userId,
            dto.studyItemId,
            true,
            ConfidenceLevel.HIGH,
            ReviewResult.MEMORIZED,
            latest,
            {
                sessionDurationMinutes: dto.sessionDurationMinutes,
                questionPositionInSession: dto.questionPositionInSession,
            },
        );

        const intervalDays = scheduled.intervalDays;
        const nextReviewAt = scheduled.nextReviewAt;

        const review = await this.repository.create({
            studyItemId: dto.studyItemId,
            result: ReviewResult.MEMORIZED,
            intervalDays,
            nextReviewAt,
            recallProbability: scheduled.fsrs.recallProbability,
            fsrsState: scheduled.fsrs.fsrsState,
            fsrsStep: scheduled.fsrs.fsrsStep,
            fsrsStability: scheduled.fsrs.fsrsStability,
            fsrsDifficulty: scheduled.fsrs.fsrsDifficulty,
            selectedOptionIndex: null,
            isCorrect: null,
            confidenceScore: null,
            responseTimeMs: dto.responseTimeMs ?? null,
            hesitationMs: null,
            answerChanges: null,
            sessionId: dto.sessionId ?? null,
            sessionDurationMinutes: dto.sessionDurationMinutes ?? null,
            questionPositionInSession: dto.questionPositionInSession ?? null,
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

    private async schedule(
        userId: string,
        studyItemId: string,
        isCorrect: boolean,
        confidence: ConfidenceLevel,
        result: ReviewResult,
        latest: Awaited<ReturnType<ReviewsRepository['findLatest']>>,
        session: {
            sessionDurationMinutes?: number;
            questionPositionInSession?: number;
        },
    ): Promise<{
        intervalDays: number;
        nextReviewAt: Date;
        fsrs: {
            recallProbability: number | null;
            fsrsState: number | null;
            fsrsStep: number | null;
            fsrsStability: number | null;
            fsrsDifficulty: number | null;
        };
    }> {
        if (this.ml.isAvailable) {
            try {
                const features = await this.buildFeatures(
                    userId,
                    studyItemId,
                    session,
                );

                const schedule = await this.ml.scheduleReview({
                    features,
                    correct: isCorrect,
                    confidence: confidence,
                    fsrs_state: latest?.fsrsState ?? null,
                    fsrs_step: latest?.fsrsStep ?? null,
                    fsrs_stability: latest?.fsrsStability ?? null,
                    fsrs_difficulty: latest?.fsrsDifficulty ?? null,
                    last_review_at: latest?.reviewedAt?.toISOString() ?? null,
                });

                return {
                    intervalDays: schedule.interval_days,
                    nextReviewAt: new Date(schedule.next_review_at),
                    fsrs: {
                        recallProbability: schedule.recall_probability,
                        fsrsState: schedule.fsrs_state,
                        fsrsStep: schedule.fsrs_step,
                        fsrsStability: schedule.fsrs_stability,
                        fsrsDifficulty: schedule.fsrs_difficulty,
                    },
                };
            } catch (error) {
                this.logger.warn(
                    `ML schedule-review failed, falling back to rule-based: ${String(error)}`,
                );
            }
        }

        const intervalDays = ReviewScheduler.calculate(
            result,
            latest?.intervalDays ?? null,
        );

        return {
            intervalDays,
            nextReviewAt: ReviewScheduler.nextReviewDate(intervalDays),
            fsrs: {
                recallProbability: null,
                fsrsState: null,
                fsrsStep: null,
                fsrsStability: null,
                fsrsDifficulty: null,
            },
        };
    }

    private async buildFeatures(
        userId: string,
        studyItemId: string,
        session: {
            sessionDurationMinutes?: number;
            questionPositionInSession?: number;
        },
    ) {
        const item = await this.repository.getStudyItemData(
            studyItemId,
            userId,
        );

        if (!item) {
            throw new NotFoundException('Study item not found.');
        }

        const reviews = await this.repository.findAllAsRows(studyItemId);

        const userStats = this.buildUserStats(
            await this.recallRepository.findUserReviewRows(userId),
        );

        return this.featureBuilder.build({
            item,
            reviews,
            userStats,
            questionGlobalRate: this.globalRate(reviews),
            now: new Date(),
            sessionDurationMinutes: session.sessionDurationMinutes ?? null,
            questionPositionInSession:
                session.questionPositionInSession ?? null,
        });
    }

    private globalRate(
        reviews: Awaited<
            ReturnType<ReviewsRepository['findAllAsRows']>
        >[number][],
    ): number | null {
        const graded = reviews.filter(
            (review) => review.isCorrect !== null,
        ).length;

        const correct = reviews.filter(
            (review) => review.isCorrect === true,
        ).length;

        return graded > 0 ? correct / graded : null;
    }

    private buildUserStats(
        rows: Awaited<ReturnType<RecallQueryRepository['findUserReviewRows']>>,
    ): UserHistoryStats {
        const stats: UserHistoryStats = {
            totalReviews: rows.length,
            correctCount: 0,
            gradedCount: 0,
            sumConfidence: 0,
            confidenceCount: 0,
            sumResponseTimeMs: 0,
            responseTimeCount: 0,
            sumHesitationMs: 0,
            hesitationCount: 0,
        };

        for (const row of rows) {
            if (row.isCorrect !== null) {
                stats.gradedCount += 1;

                if (row.isCorrect) {
                    stats.correctCount += 1;
                }
            }

            if (row.confidenceScore !== null) {
                stats.sumConfidence += row.confidenceScore;
                stats.confidenceCount += 1;
            }

            if (row.responseTimeMs !== null) {
                stats.sumResponseTimeMs += row.responseTimeMs;
                stats.responseTimeCount += 1;
            }

            if (row.hesitationMs !== null) {
                stats.sumHesitationMs += row.hesitationMs;
                stats.hesitationCount += 1;
            }
        }

        return stats;
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
