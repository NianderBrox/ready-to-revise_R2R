import { ReviewResult } from '@prisma/client';

export class ReviewScheduler {
  static calculate(
    result: ReviewResult,
  ) {
    switch (result) {
      case ReviewResult.AGAIN:
        return 1;

      case ReviewResult.HARD:
        return 3;

      case ReviewResult.GOOD:
        return 7;

      case ReviewResult.EASY:
        return 14;

      default:
        return 1;
    }
  }

  static nextReviewDate(
    intervalDays: number,
  ): Date {
    const next = new Date();

    next.setDate(
      next.getDate() + intervalDays,
    );

    return next;
  }
}