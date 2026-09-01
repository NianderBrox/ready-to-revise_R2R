import { Injectable, Logger } from '@nestjs/common';

import { ConfigService } from '@nestjs/config';

import { MlHttpService } from '../../../ml-client/infrastructure/http/ml-http.service';
import { MlConfigService } from '../../../ml-client/application/services/ml-config.service';
import {
    MlCandidate,
    MlRecommendation,
} from '../../../ml-client/domain/models/ml-contract.models';

import {
    DueQuestionRow,
    RecallReviewRow,
    UserHistoryStats,
} from '../../domain/interfaces/recall-data.interfaces';
import { RecallQueryRepository } from '../../infrastructure/repositories/recall-query.repository';
import { FeatureBuilderService } from './feature-builder.service';
import {
    RecommendationItemDto,
    RecommendationsResponseDto,
} from '../../presentation/dto/recommendation-response.dto';

const MS_PER_DAY = 24 * 60 * 60 * 1000;

const MS_PER_HOUR = 60 * 60 * 1000;

const DEFAULT_TARGET_RETENTION = 0.9;

const DEFAULT_SLIDING_WINDOW_HOURS = 48;

const DEFAULT_REFRACTORY_HOURS = 20;

const DEFAULT_DAILY_CAP = 20;

const NOMINAL_FIRST_FORGET_DAYS = 1;

const LN_AT_INTERVAL = Math.log(0.9);

interface ForgettingCandidate {
    row: DueQuestionRow;

    lastReview: RecallReviewRow | null;

    expectedForgetDate: Date;
}

@Injectable()
export class RecommendationsService {
    private readonly logger = new Logger(RecommendationsService.name);

    constructor(
        private readonly repository: RecallQueryRepository,
        private readonly featureBuilder: FeatureBuilderService,
        private readonly ml: MlHttpService,
        private readonly mlConfig: MlConfigService,
        private readonly config: ConfigService,
    ) {}

    async getRecommendations(
        userId: string,
        limit = 10,
        subjectId?: string,
    ): Promise<RecommendationsResponseDto> {
        const { candidates, meta } = await this.selectSlippingSoonCandidates(
            userId,
            subjectId,
        );

        if (candidates.length === 0) {
            return { source: 'scheduler', items: [], meta };
        }

        const effectiveLimit = Math.max(
            1,

            Math.min(limit || this.dailyCap, this.dailyCap),
        );

        const selected = candidates.slice(0, effectiveLimit);

        const reviews = await this.repository.findReviewsForItems(
            selected.map((candidate) => candidate.row.id),
        );

        const userStats = this.buildUserStats(
            await this.repository.findUserReviewRows(userId),
        );

        const now = new Date();

        const mlCandidates: MlCandidate[] = selected.map((candidate) => ({
            question_id: candidate.row.id,

            features: this.featureBuilder.build({
                item: candidate.row,

                reviews: reviews.filter(
                    (review) => review.studyItemId === candidate.row.id,
                ),

                userStats,

                questionGlobalRate: this.globalRate(reviews, candidate.row.id),

                now,
            }),
        }));

        if (!this.ml.isAvailable) {
            return this.analyticResponse(selected, meta);
        }

        try {
            const response = await this.ml.recommend({
                candidates: mlCandidates,

                top_k: selected.length,

                model_name: this.mlConfig.modelName as never,
            });

            return {
                source: 'ml',

                items: this.mapMlRanking(response.recommendations, selected),

                meta,
            };
        } catch (error) {
            this.logger.warn(
                `Falling back to forgetting-date order: ${String(error)}`,
            );

            return this.analyticResponse(selected, meta);
        }
    }

    async getAtRiskTop(userId: string): Promise<RecommendationItemDto | null> {
        const response = await this.getRecommendations(userId, 1);

        return response.items[0] ?? null;
    }

    async countSlippingSoon(userId: string): Promise<number> {
        const { candidates } = await this.selectSlippingSoonCandidates(userId);

        return candidates.length;
    }

    async getAvailabilityMeta(
        userId: string,
    ): Promise<{ restingNow: number; upcomingLater: number }> {
        const { meta } = await this.selectSlippingSoonCandidates(userId);

        return meta;
    }

    private async selectSlippingSoonCandidates(
        userId: string,
        subjectId?: string,
    ): Promise<{
        candidates: ForgettingCandidate[];

        meta: { restingNow: number; upcomingLater: number };
    }> {
        const now = new Date();

        const meta = { restingNow: 0, upcomingLater: 0 };

        const rows = await this.repository.findCandidateQuestions(
            userId,

            subjectId,
        );

        if (rows.length === 0) {
            return { candidates: [], meta };
        }

        const reviews = await this.repository.findReviewsForItems(
            rows.map((row) => row.id),
        );

        const lastReviewByItem = new Map<string, RecallReviewRow>();

        for (const review of reviews) {
            if (!lastReviewByItem.has(review.studyItemId)) {
                lastReviewByItem.set(review.studyItemId, review);
            }
        }

        const refractoryCutoff = new Date(
            now.getTime() - this.refractoryHours * MS_PER_HOUR,
        );

        const windowEnd = new Date(
            now.getTime() + this.slidingWindowHours * MS_PER_HOUR,
        );

        const eligible: ForgettingCandidate[] = [];

        for (const row of rows) {
            const lastReview = lastReviewByItem.get(row.id) ?? null;

            if (
                lastReview !== null &&
                lastReview.createdAt.getTime() >= refractoryCutoff.getTime()
            ) {
                meta.restingNow += 1;

                continue;
            }

            const expectedForgetDate = this.forgetDate(row, lastReview, now);

            if (expectedForgetDate.getTime() <= windowEnd.getTime()) {
                eligible.push({ row, lastReview, expectedForgetDate });
            } else {
                meta.upcomingLater += 1;
            }
        }

        return {
            candidates: eligible.sort(
                (a, b) =>
                    a.expectedForgetDate.getTime() -
                        b.expectedForgetDate.getTime() ||
                    (a.row.nextReviewAt?.getTime() ?? Number.MAX_SAFE_INTEGER) -
                        (b.row.nextReviewAt?.getTime() ??
                            Number.MAX_SAFE_INTEGER),
            ),

            meta,
        };
    }

    private forgetDate(
        row: DueQuestionRow,
        lastReview: RecallReviewRow | null,
        now: Date,
    ): Date {
        if (lastReview === null) {
            return new Date(
                Math.min(
                    row.createdAt.getTime() +
                        NOMINAL_FIRST_FORGET_DAYS * MS_PER_DAY,

                    now.getTime() + MS_PER_DAY,
                ),
            );
        }

        let intervalDays: number | null = null;

        if (row.nextReviewAt) {
            intervalDays = Math.max(
                0,

                (row.nextReviewAt.getTime() - lastReview.createdAt.getTime()) /
                    MS_PER_DAY,
            );
        }

        const baseDays =
            intervalDays !== null && intervalDays > 0
                ? intervalDays
                : NOMINAL_FIRST_FORGET_DAYS;

        const stretch =
            Math.log(Math.min(this.targetRetention, 0.9999)) / LN_AT_INTERVAL;

        return new Date(
            lastReview.createdAt.getTime() + baseDays * stretch * MS_PER_DAY,
        );
    }

    private analyticResponse(
        candidates: ForgettingCandidate[],

        meta?: { restingNow: number; upcomingLater: number },
    ): RecommendationsResponseDto {
        const now = Date.now();

        return {
            source: 'scheduler',

            meta,

            items: candidates.map((candidate, index) => ({
                studyItemId: candidate.row.id,

                title: candidate.row.title,

                mediaDocumentId: candidate.row.mediaDocumentId,

                options: candidate.row.options,

                expectedForgetDate: candidate.expectedForgetDate.toISOString(),

                recallProbability: null,

                priority: this.slipPriority(
                    now,

                    candidate.expectedForgetDate,
                ),

                rank: index + 1,
            })),
        };
    }

    private mapMlRanking(
        recommendations: MlRecommendation[],
        candidates: ForgettingCandidate[],
    ): RecommendationItemDto[] {
        const byId = new Map(
            candidates.map((candidate) => [candidate.row.id, candidate]),
        );

        const mapped: RecommendationItemDto[] = [];

        for (const recommendation of recommendations) {
            const candidate = byId.get(recommendation.question_id);

            if (!candidate) {
                continue;
            }

            mapped.push({
                studyItemId: candidate.row.id,

                title: candidate.row.title,

                mediaDocumentId: candidate.row.mediaDocumentId,

                options: candidate.row.options,

                expectedForgetDate: candidate.expectedForgetDate.toISOString(),

                recallProbability: recommendation.recall_probability,

                priority: (['high', 'medium', 'low'] as const).includes(
                    recommendation.priority as 'high' | 'medium' | 'low',
                )
                    ? (recommendation.priority as 'high' | 'medium' | 'low')
                    : null,

                rank: recommendation.rank,
            });
        }

        return mapped.sort((a, b) => a.rank - b.rank);
    }

    private slipPriority(
        nowMs: number,
        forgetDate: Date,
    ): 'high' | 'medium' | 'low' {
        const daysUntilSlip = (forgetDate.getTime() - nowMs) / MS_PER_DAY;

        if (daysUntilSlip <= 0) {
            return 'high';
        }

        if (daysUntilSlip <= 1) {
            return 'medium';
        }

        return 'low';
    }

    private globalRate(
        reviews: RecallReviewRow[],
        studyItemId: string,
    ): number | null {
        const scoped = reviews.filter(
            (review) => review.studyItemId === studyItemId,
        );

        const graded = scoped.filter(
            (review) => review.isCorrect !== null,
        ).length;

        const correct = scoped.filter(
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

    private get targetRetention(): number {
        const parsed = Number.parseFloat(
            String(this.config.get('RECOMMENDATION_TARGET_RETENTION') ?? ''),
        );

        return Number.isFinite(parsed) && parsed > 0 && parsed < 1
            ? parsed
            : DEFAULT_TARGET_RETENTION;
    }

    private get slidingWindowHours(): number {
        return this.positiveIntEnv(
            'RECOMMENDATION_SLIDING_WINDOW_HOURS',

            DEFAULT_SLIDING_WINDOW_HOURS,
        );
    }

    private get refractoryHours(): number {
        return this.positiveIntEnv(
            'RECOMMENDATION_REFRACTORY_HOURS',

            DEFAULT_REFRACTORY_HOURS,
        );
    }

    private get dailyCap(): number {
        return this.positiveIntEnv(
            'RECOMMENDATION_DAILY_CAP',

            DEFAULT_DAILY_CAP,
        );
    }

    private positiveIntEnv(key: string, fallback: number): number {
        const parsed = Number.parseInt(String(this.config.get(key) ?? ''), 10);

        return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
    }
}
