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
    ) {
        const parsedLimit =
            limit !== undefined ? Number.parseInt(limit, 10) : NaN;

        const safeLimit = Number.isFinite(parsedLimit)
            ? Math.min(100, Math.max(1, parsedLimit))
            : 10;

        return this.recommendationsService.getRecommendations(
            user.userId,
            safeLimit,
            subjectId !== undefined && subjectId.length > 0
                ? subjectId
                : undefined,
        );
    }
}
