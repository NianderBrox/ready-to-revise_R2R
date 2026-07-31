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
        const studyItems: StudyItemResponseDto[] = [];

        for (const command of commands) {
            studyItems.push(await this.create(command));
        }

        return studyItems;
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
        const normalizedTitle =
            dto.title !== undefined
                ? StringUtils.trim(dto.title)
                : existing.title;

        const normalizedContent =
            dto.content !== undefined
                ? StringUtils.trim(dto.content)
                : existing.content;

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
            const exists = await this.repository.topicExists(dto.topicId);

            if (!exists) {
                throw new NotFoundException('Topic not found.');
            }
        }

        const updateData = {
            title: normalizedTitle,
            content: normalizedContent,
            type: dto.type ?? existing.type,
            difficulty: dto.difficulty ?? existing.difficulty,
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
