import {
    Body,
    Controller,
    Get,
    Param,
    ParseUUIDPipe,
    Post,
} from '@nestjs/common';
import { UseGuards } from '@nestjs/common';
import { CurrentUser } from '../../../../common/decorators/current-user.decorator';
import type { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';
import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';
import { ReviewsService } from '../../application/services/reviews.service';
import { CreateReviewDto } from '../dto/create-review.dto';

@Controller('reviews')
@UseGuards(JwtAuthGuard)
export class ReviewsController {
    constructor(private readonly reviewsService: ReviewsService) {}

    async create(
        @CurrentUser()
        user: CurrentUserData,

        @Body()
        dto: CreateReviewDto,
    ) {
        return this.reviewsService.create(user.userId, dto);
    }

    async history(
        @CurrentUser()
        user: CurrentUserData,

        @Param('studyItemId', ParseUUIDPipe)
        studyItemId: string,
    ) {
        return this.reviewsService.history(user.userId, studyItemId);
    }
}
