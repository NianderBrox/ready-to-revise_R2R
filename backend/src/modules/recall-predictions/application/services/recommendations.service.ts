import { Injectable } from '@nestjs/common';

import { ConfigService } from '@nestjs/config';

import { RecallQueryRepository } from '../../infrastructure/repositories/recall-query.repository';
import {
    RecommendationItemDto,
    RecommendationsResponseDto,
} from '../../presentation/dto/recommendation-response.dto';

const DEFAULT_DAILY_CAP = 20;

@Injectable()
export class RecommendationsService {
    constructor(
        private readonly repository: RecallQueryRepository,
        private readonly config: ConfigService,
    ) {}

    async getRecommendations(
        userId: string,
        limit = 10,
        subjectId?: string,
        dueBefore?: Date,
    ): Promise<RecommendationsResponseDto> {
        const rows = await this.repository.findDueQuestions(
            userId,
            subjectId,
            dueBefore,
        );

        const effectiveLimit = Math.max(
            1,

            Math.min(limit || this.dailyCap, this.dailyCap),
        );

        const items: RecommendationItemDto[] = rows
            .slice(0, effectiveLimit)
            .map((row, index) => ({
                studyItemId: row.id,

                title: row.title,

                mediaDocumentId: row.mediaDocumentId,

                options: row.options,

                nextReviewAt: row.nextReviewAt?.toISOString() ?? null,

                rank: index + 1,
            }));

        return { source: 'scheduler', items };
    }

    async getAtRiskTop(
        userId: string,
        dueBefore?: Date,
    ): Promise<RecommendationItemDto | null> {
        const response = await this.getRecommendations(
            userId,
            1,
            undefined,
            dueBefore,
        );

        return response.items[0] ?? null;
    }

    async countSlippingSoon(userId: string, dueBefore?: Date): Promise<number> {
        return this.repository.countUserDueQuestions(userId, dueBefore);
    }

    private get dailyCap(): number {
        const parsed = Number.parseInt(
            String(this.config.get('RECOMMENDATION_DAILY_CAP') ?? ''),
            10,
        );

        return Number.isFinite(parsed) && parsed > 0
            ? parsed
            : DEFAULT_DAILY_CAP;
    }
}
