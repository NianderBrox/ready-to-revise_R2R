import { Controller, Get, Query, UseGuards } from '@nestjs/common';

import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';
import { CurrentUser } from '../../../../common/decorators/current-user.decorator';
import type { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';

import { RecommendationsService } from '../../application/services/recommendations.service';

@Controller('recommendations')
@UseGuards(JwtAuthGuard)
export class RecommendationsController {
    constructor(
        private readonly recommendationsService: RecommendationsService,
    ) {}

    @Get()
    async getRecommendations(
        @CurrentUser() user: CurrentUserData,
        @Query('limit') limit?: string,
        @Query('subjectId') subjectId?: string,
        @Query('dueBefore') dueBefore?: string,
    ) {
        const parsedLimit =
            limit !== undefined ? Number.parseInt(limit, 10) : NaN;

        const safeLimit = Number.isFinite(parsedLimit)
            ? Math.min(100, Math.max(1, parsedLimit))
            : 10;

        const parsedDueBefore =
            dueBefore !== undefined && dueBefore.length > 0
                ? new Date(dueBefore)
                : undefined;

        return this.recommendationsService.getRecommendations(
            user.userId,
            safeLimit,
            subjectId !== undefined && subjectId.length > 0
                ? subjectId
                : undefined,
            parsedDueBefore !== undefined &&
                !Number.isNaN(parsedDueBefore.getTime())
                ? parsedDueBefore
                : undefined,
        );
    }
}
