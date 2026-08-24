import { Injectable } from '@nestjs/common';

import {
    DueQuestionRow,
    RecallReviewRow,
    UserHistoryStats,
} from '../../domain/interfaces/recall-data.interfaces';

import { ReviewFeatures } from '../../../ml-client/domain/models/ml-contract.models';

const MS_PER_SECOND = 1000;

const SECONDS_LIMIT_RESPONSE = 120.0;

const SECONDS_LIMIT_HESITATION = 30.0;

const MAX_INTERVAL_DAYS = 365.0;

const MAX_REPETITION_NUMBER = 20;

const RECENT_WINDOW = 5;

const RETRIEVABILITY_AT_INTERVAL = 0.9;

const MS_PER_DAY = 24 * 60 * 60 * 1000;

const DIFFICULTY_SCORES: Record<string, number> = {
    EASY: 1,
    MEDIUM: 2,
    HARD: 3,
};

@Injectable()
export class FeatureBuilderService {
    build(input: {
        item: DueQuestionRow;
        reviews: RecallReviewRow[];
        userStats: UserHistoryStats;
        questionGlobalRate: number | null;
        now: Date;
    }): ReviewFeatures {
        const { item, reviews, userStats, now } = input;

        const last = reviews[0] ?? null;

        const avgResponseSec =
            userStats.responseTimeCount > 0
                ? userStats.sumResponseTimeMs /
                  userStats.responseTimeCount /
                  MS_PER_SECOND
                : null;

        const avgHesitationSec =
            userStats.hesitationCount > 0
                ? userStats.sumHesitationMs /
                  userStats.hesitationCount /
                  MS_PER_SECOND
                : null;

        const intervalDays = this.intervalDays(item.nextReviewAt, last);

        const questionText = item.title ?? '';

        const recent = reviews.slice(0, RECENT_WINDOW);

        const fsrs = this.retrievability(last, intervalDays, now);

        return {
            total_revisions: reviews.length,

            success_rate: this.safeRate(
                this.countCorrect(reviews),
                reviews.length,
            ),

            average_confidence:
                userStats.confidenceCount > 0
                    ? userStats.sumConfidence / userStats.confidenceCount
                    : null,

            average_response_time: avgResponseSec,

            average_hesitation: avgHesitationSec,

            difficulty: DIFFICULTY_SCORES[item.difficulty ?? ''] ?? 2,

            word_count: this.wordCount(questionText),

            character_count: questionText.length,

            session_duration_minutes: 0,

            question_position_in_session: 1,

            days_since_last_session: last
                ? (now.getTime() - last.createdAt.getTime()) / MS_PER_DAY
                : null,

            review_interval_days: intervalDays,

            repetition_number: reviews.length,

            last_review_confidence_score: last?.confidenceScore ?? null,

            last_review_response_time:
                last?.responseTimeMs != null
                    ? last.responseTimeMs / MS_PER_SECOND
                    : null,

            last_review_hesitation:
                last?.hesitationMs != null
                    ? last.hesitationMs / MS_PER_SECOND
                    : null,

            answer_changes: last?.answerChanges ?? 0,

            subject: item.subjectName ?? 'unknown',

            topic: item.topicName ?? 'unknown',

            hour_of_day: now.getHours(),

            day_of_week: (now.getDay() + 6) % 7,

            last_review_correct: last?.isCorrect ?? null,

            fsrs_recall_probability: fsrs,

            had_fsrs_estimate: fsrs !== null,

            user_success_rate: this.safeRate(
                userStats.correctCount,
                userStats.gradedCount,
            ),

            user_average_confidence:
                userStats.confidenceCount > 0
                    ? userStats.sumConfidence / userStats.confidenceCount
                    : null,

            user_average_response_time: avgResponseSec,

            question_global_success_rate: input.questionGlobalRate,

            recent_success_rate_5: this.safeRate(
                this.countCorrect(recent),
                recent.length,
            ),

            recent_confidence_5: this.mean(
                recent
                    .map((review) => review.confidenceScore)
                    .filter((value): value is number => value !== null),
            ),

            consecutive_correct: this.consecutiveCorrect(reviews),

            hesitation_response_ratio:
                avgHesitationSec !== null &&
                avgResponseSec !== null &&
                avgResponseSec > 0
                    ? avgHesitationSec / avgResponseSec
                    : null,

            normalized_interval_days:
                intervalDays !== null
                    ? this.clip01(intervalDays / MAX_INTERVAL_DAYS)
                    : null,

            normalized_repetition_number: this.clip01(
                reviews.length / MAX_REPETITION_NUMBER,
            ),

            normalized_avg_response_time:
                avgResponseSec !== null
                    ? this.clip01(avgResponseSec / SECONDS_LIMIT_RESPONSE)
                    : null,

            normalized_avg_hesitation:
                avgHesitationSec !== null
                    ? this.clip01(avgHesitationSec / SECONDS_LIMIT_HESITATION)
                    : null,
        };
    }

    private intervalDays(
        nextReviewAt: Date | null,
        last: RecallReviewRow | null,
    ): number | null {
        if (!nextReviewAt || !last) {
            return null;
        }

        const raw =
            (nextReviewAt.getTime() - last.createdAt.getTime()) / MS_PER_DAY;

        return Math.max(0, raw);
    }

    private retrievability(
        last: RecallReviewRow | null,
        intervalDays: number | null,
        now: Date,
    ): number | null {
        if (!last || intervalDays === null || intervalDays <= 0) {
            return null;
        }

        const elapsedDays =
            (now.getTime() - last.createdAt.getTime()) / MS_PER_DAY;

        if (elapsedDays < 0) {
            return null;
        }

        return Math.pow(RETRIEVABILITY_AT_INTERVAL, elapsedDays / intervalDays);
    }

    private countCorrect(reviews: RecallReviewRow[]): number {
        return reviews.filter((review) => review.isCorrect === true).length;
    }

    private safeRate(numerator: number, denominator: number): number | null {
        return denominator > 0 ? numerator / denominator : null;
    }

    private mean(values: number[]): number | null {
        if (values.length === 0) {
            return null;
        }

        return values.reduce((sum, value) => sum + value, 0) / values.length;
    }

    private consecutiveCorrect(reviews: RecallReviewRow[]): number {
        let streak = 0;

        for (const review of reviews) {
            if (review.isCorrect !== true) {
                break;
            }

            streak += 1;
        }

        return streak;
    }

    private wordCount(text: string): number {
        const trimmed = text.trim();

        return trimmed.length === 0 ? 0 : trimmed.split(/\s+/).length;
    }

    private clip01(value: number): number {
        return Math.min(1, Math.max(0, value));
    }
}
