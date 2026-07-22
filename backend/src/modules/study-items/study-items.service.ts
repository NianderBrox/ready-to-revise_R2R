import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { StudyItemsRepository } from './study-items.repository';
import { CreateStudyItemDto } from './dto/create-study-item.dto';
import { StudyItemResponseDto } from './dto/study-item-response.dto';
import { StudyItemsMapper } from './study-items.mapper';
// import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class StudyItemsService {
    constructor(
        private readonly repository: StudyItemsRepository,
        // private readonly prisma: PrismaService,
    ) {}

    async create(
      userId: string,
      dto: CreateStudyItemDto,
    ): Promise<StudyItemResponseDto> {

      if (!dto.title && !dto.content) {
        throw new BadRequestException(
          'Either title or content is required.',
        );
      }

      if (dto.topicId !== undefined) {
        const exists = await this.repository.topicExists(dto.topicId);

        if (!exists) {
          throw new NotFoundException('Topic not found.');
        }
      }

      const studyItem = await this.repository.create({
        title: dto.title,
        content: dto.content,
        type: dto.type,
        difficulty: dto.difficulty,
        topicId: dto.topicId,
        userId,
      });

      return StudyItemsMapper.toResponse(studyItem);
    }
}
