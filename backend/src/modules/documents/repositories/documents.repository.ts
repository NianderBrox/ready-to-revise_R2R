import { Injectable } from '@nestjs/common';

import { Document, Prisma } from '@prisma/client';

import { PrismaService } from '../../../prisma/prisma.service';

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
        });
    }

    findById(id: string) {
        return this.prisma.document.findUnique({
            where: {
                id,
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
        return this.prisma.document.update({
            where: {
                id,
            },
            data,
        });
    }

    findManyByUserId(userId: string) {
        return this.prisma.document.findMany({
            where: {
                userId,
            },

            orderBy: {
                createdAt: 'desc',
            },
        });
    }
}
