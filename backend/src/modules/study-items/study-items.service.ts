import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { StudyItemsRepository } from './study-items.repository';
import { CreateStudyItemDto } from './dto/create-study-item.dto';
import { StudyItemResponseDto } from './dto/study-item-response.dto';
import { StudyItemsMapper } from './study-items.mapper';
import { UpdateStudyItemDto } from './dto/update-study-item.dto';
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
        const normalizedTitle =
        dto.title?.trim();

        const normalizedContent =
        dto.content?.trim();

    if (!normalizedTitle && !normalizedContent) {
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

    async findAll(
    userId: string,
    ): Promise<StudyItemResponseDto[]> {

        const studyItems =
            await this.repository.findAllByUser(userId);

        return studyItems.map((studyItem) =>
            StudyItemsMapper.toResponse(studyItem),
        );
    }

    async findOne(
    id: string,
    userId: string,
    ): Promise<StudyItemResponseDto> {

        const studyItem = await this.repository.findById(
            id,
            userId,
        );

        if (!studyItem) {
            throw new NotFoundException('Study item not found.');
        }

        return StudyItemsMapper.toResponse(studyItem);
    }

    async update(
    id: string,
    userId: string,
    dto: UpdateStudyItemDto,
    ): Promise<StudyItemResponseDto> {
        const normalizedTitle =
        dto.title?.trim();

        const normalizedContent =
        dto.content?.trim();

        const existing = await this.repository.findById(
            id,
            userId,
        );

        if (!existing) {
            throw new NotFoundException(
            'Study item not found.',
            );
        }

        if (
            normalizedTitle !== undefined &&
            normalizedContent !== undefined &&
            normalizedTitle === '' &&
            normalizedContent === ''
        ) {
            throw new BadRequestException(
                'Title and content cannot both be empty.',
            );
        }

        if (dto.topicId) {
            const exists = await this.repository.topicExists(
            dto.topicId,
            );

            if (!exists) {
            throw new NotFoundException(
                'Topic not found.',
            );
            }
        }

        const updateData = {
            ...dto,
            title: normalizedTitle,
            content: normalizedContent,
        };

        const updated = await this.repository.update(id,updateData,);

        return StudyItemsMapper.toResponse(updated);
    }

    async remove(
        id: string,
        userId: string,
        ): Promise<void> {
        const studyItem = await this.repository.findById(
            id,
            userId,
        );

        if (!studyItem) {
            throw new NotFoundException(
            'Study item not found.',
            );
        }

        await this.repository.delete(id);
    }
}
