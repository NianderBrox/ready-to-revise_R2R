import {
    BadRequestException,
    ConflictException,
    Injectable,
    NotFoundException,
} from '@nestjs/common';

import { SubjectsRepository } from './subjects.repository';

import { CreateSubjectDto } from './dto/create-subject.dto';
import { SubjectResponseDto } from './dto/subject-response.dto';

import { SubjectsMapper } from './subjects.mapper';
import { StringUtils } from '../../common/utils/string.utils';
import { UpdateSubjectDto } from './dto/update-subject.dto';

@Injectable()
export class SubjectsService {
    constructor(private readonly repository: SubjectsRepository) {}

    async create(dto: CreateSubjectDto): Promise<SubjectResponseDto> {
        if (!dto.name) {
            throw new BadRequestException('Name is required.');
        }

        const normalizedName = StringUtils.normalizeSubjectName(dto.name);

        const existing = await this.repository.findByNormalizedName(
            normalizedName!,
        );

        if (existing) {
            throw new ConflictException('Subject already exists.');
        }

        const subject = await this.repository.create({
            name: normalizedName,
        });

        return SubjectsMapper.toResponse(subject);
    }

    async findAll(): Promise<SubjectResponseDto[]> {
        const subjects = await this.repository.findAll();

        return subjects.map((subject) => SubjectsMapper.toResponse(subject));
    }

    async findOne(id: string): Promise<SubjectResponseDto> {
        const subject = await this.repository.findById(id);

        if (!subject) {
            throw new NotFoundException('Subject not found.');
        }

        return SubjectsMapper.toResponse(subject);
    }

    async update(
        id: string,
        dto: UpdateSubjectDto,
    ): Promise<SubjectResponseDto> {
        const subject = await this.repository.findById(id);

        if (!subject) {
            throw new NotFoundException('Subject not found.');
        }

        const normalizedName =
            dto.name !== undefined
                ? StringUtils.normalizeName(dto.name)
                : subject.name;

        const existing =
            await this.repository.findByNormalizedName(normalizedName);

        if (existing && existing.id !== id) {
            throw new ConflictException('Subject already exists.');
        }

        const updated = await this.repository.update(id, normalizedName);

        return SubjectsMapper.toResponse(updated);
    }

    async remove(id: string): Promise<void> {
        const subject = await this.repository.findById(id);

        if (!subject) {
            throw new NotFoundException('Subject not found.');
        }

        await this.repository.delete(id);
    }
}
