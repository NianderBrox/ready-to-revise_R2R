import { Controller, Get, Query, UseGuards } from '@nestjs/common';

import { AnalyticsService } from '../../application/services/analytics.service';

import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';

import { CurrentUser } from '../../../../common/decorators/current-user.decorator';

import type { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';

@Controller('analytics')
@UseGuards(JwtAuthGuard)
export class AnalyticsController {
    constructor(private readonly analyticsService: AnalyticsService) {}

    @Get('overview')
    async getOverview(
        @CurrentUser()
        user: CurrentUserData,

        @Query('range')
        range?: string,
    ) {
        return this.analyticsService.getOverview(user.userId, range);
    }

    @Get('heatmap')
    async getHeatmap(
        @CurrentUser()
        user: CurrentUserData,
    ) {
        return this.analyticsService.getHeatmap(user.userId);
    }
}
