import { Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../prisma/prisma.service';
import { CreateChapterData } from '../../domain/interfaces/create-chapter-data.interface';

@Injectable()
export class ChaptersRepository {
    constructor(private readonly prisma: PrismaService) {}

    async create(data: CreateChapterData) {
        return this.prisma.chapter.create({
            data,
        });
    }

    async findAll() {
        return this.prisma.chapter.findMany({
            orderBy: [
                {
                    subject: {
                        name: 'asc',
                    },
                },
                {
                    name: 'asc',
                },
            ],
        });
    }

    async findById(id: string) {
        return this.prisma.chapter.findUnique({
            where: {
                id,
            },
        });
    }

    async findByNameAndSubject(name: string, subjectId: string) {
        return this.prisma.chapter.findUnique({
            where: {
                subjectId_name: {
                    subjectId,
                    name,
                },
            },
        });
    }

    async update(id: string, data: CreateChapterData) {
        return this.prisma.chapter.update({
            where: {
                id,
            },
            data,
        });
    }

    async delete(id: string) {
        await this.prisma.chapter.delete({
            where: {
                id,
            },
        });
    }

    async subjectExists(subjectId: string): Promise<boolean> {
        const subject = await this.prisma.subject.findUnique({
            where: {
                id: subjectId,
            },
            select: {
                id: true,
            },
        });

        return !!subject;
    }
}
