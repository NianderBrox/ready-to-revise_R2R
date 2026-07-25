import {
  Body,
  Controller,
  Get,
  Param,
  ParseUUIDPipe,
  Post,
} from '@nestjs/common';

import { ReviewsService } from './reviews.service';

import { CreateReviewDto } from './dto/create-review.dto';

import { UseGuards } from '@nestjs/common';

import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

import { CurrentUser } from '../../common/decorators/current-user.decorator';

import type { CurrentUserData } from '../../common/interfaces/current-user-data.interface';

@Controller('reviews')
@UseGuards(JwtAuthGuard)
export class ReviewsController {
  constructor(
    private readonly reviewsService: ReviewsService,
  ) {}

    async create(
        @CurrentUser()
        user: CurrentUserData,

        @Body()
        dto: CreateReviewDto,
    ) {
        return this.reviewsService.create(
            user.userId,
            dto,
        );
    }

    async history(
    @CurrentUser()
    user: CurrentUserData,

    @Param(
        'studyItemId',
        ParseUUIDPipe,
    )
        studyItemId: string,
        ) {
        return this.reviewsService.history(
            user.userId,
            studyItemId,
        );
    }
}