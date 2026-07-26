import { Injectable } from '@nestjs/common';

import { Document, Prisma } from '@prisma/client';

import { PrismaService } from '../../../prisma/prisma.service';

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
}
