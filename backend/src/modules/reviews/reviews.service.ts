import {
  Injectable,
  NotFoundException,
} from '@nestjs/common';

import { ReviewsRepository } from './reviews.repository';

import { CreateReviewDto } from './dto/create-review.dto';

import { ReviewsMapper } from './reviews.mapper';

import { ReviewScheduler } from './utils/review-scheduler';

import { ReviewResponseDto } from './dto/review-response.dto';

@Injectable()
export class ReviewsService {
  constructor(
    private readonly repository: ReviewsRepository,
  ) {}

  async create(
    userId: string,
    dto: CreateReviewDto,
  ): Promise<ReviewResponseDto> {

    const exists =
      await this.repository.studyItemExists(
        dto.studyItemId,
        userId,
      );

    if (!exists) {
      throw new NotFoundException(
        'Study item not found.',
      );
    }

    const intervalDays =
      ReviewScheduler.calculate(
        dto.result,
      );

    const nextReviewAt =
      ReviewScheduler.nextReviewDate(
        intervalDays,
      );

    const review =
      await this.repository.create({
        studyItemId: dto.studyItemId,
        result: dto.result,
        intervalDays,
        nextReviewAt,
      });

    return ReviewsMapper.toResponse(review);
  }

  async history(
    studyItemId: string,
    userId: string,
  ): Promise<ReviewResponseDto[]> {

    const exists =
      await this.repository.studyItemExists(
        studyItemId,
        userId
      );

    if (!exists) {
      throw new NotFoundException(
        'Study item not found or access denied.',
      );
    }

    const reviews =
      await this.repository.findAllByStudyItem(
        studyItemId,
      );

    return reviews.map(ReviewsMapper.toResponse);
  }
}