import { Chapter } from '@prisma/client';

import { ChapterResponseDto } from './dto/chapter-response.dto';

export class ChaptersMapper {
  static toResponse(
    chapter: Chapter,
  ): ChapterResponseDto {
    return {
      id: chapter.id,
      name: chapter.name,
      subjectId: chapter.subjectId,
      createdAt: chapter.createdAt,
      updatedAt: chapter.updatedAt,
    };
  }
}