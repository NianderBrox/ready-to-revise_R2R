import { Controller, Get, Query, UseGuards } from '@nestjs/common';

import { DashboardService } from '../../application/services/dashboard.service';

import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';

import { CurrentUser } from '../../../../common/decorators/current-user.decorator';

import type { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';

@Controller('dashboard')
@UseGuards(JwtAuthGuard)
export class DashboardController {
    constructor(private readonly dashboardService: DashboardService) {}

    @Get()
    async getDashboard(
        @CurrentUser()
        user: CurrentUserData,

        @Query('dueBefore')
        dueBefore?: string,
    ) {
        const parsedDueBefore =
            dueBefore !== undefined && dueBefore.length > 0
                ? new Date(dueBefore)
                : undefined;

        return this.dashboardService.getDashboard(
            user.userId,

            parsedDueBefore !== undefined &&
                !Number.isNaN(parsedDueBefore.getTime())
                ? parsedDueBefore
                : undefined,
        );
    }
}
