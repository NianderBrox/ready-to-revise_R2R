import {
    BadRequestException,
    ConflictException,
    Injectable,
    NotFoundException,
} from '@nestjs/common';

import { ChaptersRepository } from './chapters.repository';

import { ChaptersMapper } from './chapters.mapper';

import { CreateChapterDto } from './dto/create-chapter.dto';
import { UpdateChapterDto } from './dto/update-chapter.dto';
import { ChapterResponseDto } from './dto/chapter-response.dto';

import { StringUtils } from '../../common/utils/string.utils';

@Injectable()
export class ChaptersService {
    constructor(private readonly repository: ChaptersRepository) {}

    async create(dto: CreateChapterDto): Promise<ChapterResponseDto> {
        const normalizedName = StringUtils.normalizeName(dto.name);

        const subjectExists = await this.repository.subjectExists(
            dto.subjectId,
        );

        if (!subjectExists) {
            throw new NotFoundException('Subject not found.');
        }

        const existing = await this.repository.findByNameAndSubject(
            normalizedName,
            dto.subjectId,
        );

        if (existing) {
            throw new ConflictException(
                'Chapter already exists in this subject.',
            );
        }

        const chapter = await this.repository.create({
            name: normalizedName,
            subjectId: dto.subjectId,
        });

        return ChaptersMapper.toResponse(chapter);
    }

    async findAll(): Promise<ChapterResponseDto[]> {
        const chapters = await this.repository.findAll();

        return chapters.map((chapter) => ChaptersMapper.toResponse(chapter));
    }

    async findOne(id: string): Promise<ChapterResponseDto> {
        const chapter = await this.repository.findById(id);

        if (!chapter) {
            throw new NotFoundException('Chapter not found.');
        }

        return ChaptersMapper.toResponse(chapter);
    }

    async update(
        id: string,
        dto: UpdateChapterDto,
    ): Promise<ChapterResponseDto> {
        const chapter = await this.repository.findById(id);

        if (!chapter) {
            throw new NotFoundException('Chapter not found.');
        }

        const normalizedName =
            dto.name !== undefined
                ? StringUtils.normalizeName(dto.name)
                : chapter.name;

        const subjectId = dto.subjectId ?? chapter.subjectId;

        const subjectExists = await this.repository.subjectExists(subjectId);

        if (!subjectExists) {
            throw new NotFoundException('Subject not found.');
        }

        const existing = await this.repository.findByNameAndSubject(
            normalizedName,
            subjectId,
        );

        if (existing && existing.id !== id) {
            throw new ConflictException(
                'Chapter already exists in this subject.',
            );
        }

        const updated = await this.repository.update(id, {
            name: normalizedName,
            subjectId,
        });

        return ChaptersMapper.toResponse(updated);
    }

    async remove(id: string): Promise<void> {
        const chapter = await this.repository.findById(id);

        if (!chapter) {
            throw new NotFoundException('Chapter not found.');
        }

        await this.repository.delete(id);
    }
}
