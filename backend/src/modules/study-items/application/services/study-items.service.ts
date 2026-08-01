import {
    BadRequestException,
    Injectable,
    NotFoundException,
} from '@nestjs/common';
import { StringUtils } from '../../../../common/utils/string.utils';
import { StudyItemsRepository } from '../../infrastructure/repositories/study-items.repository';
import { StudyItemResponseDto } from '../../presentation/dto/study-item-response.dto';
import { UpdateStudyItemDto } from '../../presentation/dto/update-study-item.dto';
import { CreateStudyItemCommand } from '../commands/create-study-item.command';
import { StudyItemsMapper } from '../mappers/study-items.mapper';
import { CreateStudyItemData } from '../../domain/interfaces/create-study-item-data.interface';

@Injectable()
export class StudyItemsService {
    constructor(private readonly repository: StudyItemsRepository) {}

    async create(
        command: CreateStudyItemCommand,
    ): Promise<StudyItemResponseDto> {
        const normalizedTitle = StringUtils.trim(command.title);

        const normalizedContent = StringUtils.trim(command.content);

        if (!normalizedTitle && !normalizedContent) {
            throw new BadRequestException(
                'Either title or content is required.',
            );
        }

        if (command.topicId !== undefined) {
            const exists = await this.repository.topicExists(command.topicId);

            if (!exists) {
                throw new NotFoundException('Topic not found.');
            }
        }

        const studyItem = await this.repository.create({
            title: normalizedTitle,
            content: normalizedContent,
            type: command.type,
            difficulty: command.difficulty,
            topicId: command.topicId,
            userId: command.userId,
        });

        return StudyItemsMapper.toResponse(studyItem);
    }

    async createMany(
        commands: CreateStudyItemCommand[],
    ): Promise<StudyItemResponseDto[]> {
        const data: CreateStudyItemData[] = [];

        for (const command of commands) {
            const normalizedTitle = StringUtils.trim(command.title);
            const normalizedContent = StringUtils.trim(command.content);

            if (!normalizedTitle && !normalizedContent) {
                throw new BadRequestException(
                    'Either title or content is required.',
                );
            }

            if (command.topicId !== undefined) {
                const exists = await this.repository.topicExists(
                    command.topicId,
                );

                if (!exists) {
                    throw new NotFoundException('Topic not found.');
                }
            }

            data.push({
                title: normalizedTitle,
                content: normalizedContent,
                type: command.type,
                difficulty: command.difficulty,
                topicId: command.topicId,
                userId: command.userId,
            });
        }

        const studyItems = await this.repository.createMany(data);

        return studyItems.map((studyItem) =>
            StudyItemsMapper.toResponse(studyItem),
        );
    }

    async findAll(userId: string): Promise<StudyItemResponseDto[]> {
        const studyItems = await this.repository.findAllByUser(userId);

        return studyItems.map((studyItem) =>
            StudyItemsMapper.toResponse(studyItem),
        );
    }

    async findOne(id: string, userId: string): Promise<StudyItemResponseDto> {
        const studyItem = await this.repository.findById(id, userId);

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
        const existing = await this.repository.findById(id, userId);

        if (!existing) {
            throw new NotFoundException('Study item not found.');
        }

        if (dto.topicId !== undefined) {
            const exists = await this.repository.topicExists(dto.topicId);

            if (!exists) {
                throw new NotFoundException('Topic not found.');
            }
        }

        const updateData = {
            difficulty:
                dto.difficulty !== undefined
                    ? dto.difficulty
                    : existing.difficulty,

            topicId: dto.topicId !== undefined ? dto.topicId : existing.topicId,
        };

        const updated = await this.repository.update(id, updateData);

        return StudyItemsMapper.toResponse(updated);
    }

    async remove(id: string, userId: string): Promise<void> {
        const studyItem = await this.repository.findById(id, userId);

        if (!studyItem) {
            throw new NotFoundException('Study item not found.');
        }

        await this.repository.delete(id);
    }
}
