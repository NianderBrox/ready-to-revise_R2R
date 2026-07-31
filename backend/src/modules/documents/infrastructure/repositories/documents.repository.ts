import { Injectable } from '@nestjs/common';
import { Document, DocumentStatus, Prisma } from '@prisma/client';
import { PrismaService } from '../../../../prisma/prisma.service';
import { DocumentWithMetadata } from '../../domain/types/document.types';
import { DocumentAnalysisResult } from '../../../document-analysis/domain/models/document-analysis.result';

type AttachStorageData = {
    storageKey: string;
    fileSize: number;
    checksum?: string;
};

@Injectable()
export class DocumentsRepository {
    constructor(private readonly prisma: PrismaService) {}

    create(data: {
        userId: string;
        originalName: string;
        mimeType: string;
        status: Document['status'];
    }) {
        return this.prisma.document.create({
            data,
        });
    }

    findByIdAndUserId(id: string, userId: string) {
        return this.prisma.document.findFirst({
            where: {
                id,
                userId,
            },
            include: {
                metadata: true,
            },
        });
    }

    async findById(id: string): Promise<DocumentWithMetadata | null> {
        return this.prisma.document.findUnique({
            where: {
                id,
            },
            include: {
                metadata: true,
            },
        });
    }

    update(id: string, data: Prisma.DocumentUpdateInput) {
        return this.prisma.document.update({
            where: {
                id,
            },
            data,
        });
    }

    delete(id: string) {
        return this.prisma.document.delete({
            where: {
                id,
            },
        });
    }

    attachStorage(id: string, data: AttachStorageData) {
        return this.update(id, data);
    }

    findManyByUserId(userId: string) {
        return this.prisma.document.findMany({
            where: {
                userId,
            },

            orderBy: {
                createdAt: 'desc',
            },
            include: {
                metadata: true,
            },
        });
    }

    async markReady(id: string, analysis: DocumentAnalysisResult) {
        return this.prisma.$transaction(async (tx) => {
            const document = await tx.document.update({
                where: {
                    id,
                },
                data: {
                    title: analysis.title,
                    status: DocumentStatus.READY,
                },
            });

            await tx.documentMetadata.upsert({
                where: {
                    documentId: id,
                },
                create: {
                    documentId: id,
                    summary: analysis.summary,
                    subject: analysis.subject,
                    chapter: analysis.chapter,
                    topic: analysis.topic,
                    difficulty: analysis.difficulty,
                    keywords: analysis.keywords,
                },
                update: {
                    summary: analysis.summary,
                    subject: analysis.subject,
                    chapter: analysis.chapter,
                    topic: analysis.topic,
                    difficulty: analysis.difficulty,
                    keywords: analysis.keywords,
                },
            });

            return document;
        });
    }
}
