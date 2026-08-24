import { Injectable } from '@nestjs/common';

export const CONFIDENCE_WEIGHTS = {
    correct: 0.4,
    responseTime: 0.25,
    hesitation: 0.2,
    answerChange: 0.15,
} as const;

export const SIMULATION_LIMITS = {
    maxResponseTimeSeconds: 120.0,
    maxHesitationSeconds: 30.0,
    maxAnswerChanges: 5,
} as const;

export const LOW_CONFIDENCE_THRESHOLD = 0.4;

export const HIGH_CONFIDENCE_THRESHOLD = 0.75;

export enum ConfidenceLevel {
    LOW = 'LOW',
    MEDIUM = 'MEDIUM',
    HIGH = 'HIGH',
}

export interface ConfidenceObservables {
    isCorrect: boolean;

    responseTimeMs?: number | null;

    hesitationMs?: number | null;

    answerChanges?: number | null;
}

export interface ConfidenceResult {
    score: number;

    level: ConfidenceLevel;
}

@Injectable()
export class ConfidenceInferenceService {
    infer(observables: ConfidenceObservables): ConfidenceResult {
        this.validate(observables);

        const score = this.computeScore(observables);

        return {
            score: Math.round(score * 10000) / 10000,

            level: this.categorize(score),
        };
    }

    private computeScore(observables: ConfidenceObservables): number {
        const correctScore = observables.isCorrect ? 1.0 : 0.0;

        const responseScore = this.normalizeResponseTime(
            observables.responseTimeMs,
        );

        const hesitationScore = this.normalizeHesitation(
            observables.hesitationMs,
        );

        const answerChangeScore = this.normalizeAnswerChanges(
            observables.answerChanges,
        );

        return (
            CONFIDENCE_WEIGHTS.correct * correctScore +
            CONFIDENCE_WEIGHTS.responseTime * responseScore +
            CONFIDENCE_WEIGHTS.hesitation * hesitationScore +
            CONFIDENCE_WEIGHTS.answerChange * answerChangeScore
        );
    }

    private normalizeResponseTime(responseTimeMs?: number | null): number {
        if (responseTimeMs === undefined || responseTimeMs === null) {
            return this.missingValueScore();
        }

        const seconds = responseTimeMs / 1000.0;

        return (
            1.0 -
            Math.min(seconds / SIMULATION_LIMITS.maxResponseTimeSeconds, 1.0)
        );
    }

    private normalizeHesitation(hesitationMs?: number | null): number {
        if (hesitationMs === undefined || hesitationMs === null) {
            return this.missingValueScore();
        }

        const seconds = hesitationMs / 1000.0;

        return (
            1.0 -
            Math.min(seconds / SIMULATION_LIMITS.maxHesitationSeconds, 1.0)
        );
    }

    private normalizeAnswerChanges(answerChanges?: number | null): number {
        if (answerChanges === undefined || answerChanges === null) {
            return this.missingValueScore();
        }

        return (
            1.0 -
            Math.min(answerChanges / SIMULATION_LIMITS.maxAnswerChanges, 1.0)
        );
    }

    private missingValueScore(): number {
        return 0.5;
    }

    private categorize(score: number): ConfidenceLevel {
        if (score < LOW_CONFIDENCE_THRESHOLD) {
            return ConfidenceLevel.LOW;
        }

        if (score < HIGH_CONFIDENCE_THRESHOLD) {
            return ConfidenceLevel.MEDIUM;
        }

        return ConfidenceLevel.HIGH;
    }

    private validate(observables: ConfidenceObservables): void {
        if (
            observables.responseTimeMs !== undefined &&
            observables.responseTimeMs !== null &&
            observables.responseTimeMs < 0
        ) {
            throw new Error('Response time cannot be negative.');
        }

        if (
            observables.hesitationMs !== undefined &&
            observables.hesitationMs !== null &&
            observables.hesitationMs < 0
        ) {
            throw new Error('Hesitation cannot be negative.');
        }

        if (
            observables.answerChanges !== undefined &&
            observables.answerChanges !== null &&
            observables.answerChanges < 0
        ) {
            throw new Error('Answer changes cannot be negative.');
        }
    }
}
