import { Injectable, NotFoundException } from '@nestjs/common';

import { Document } from '@prisma/client';

import { DocumentsRepository } from '../../repositories/documents.repository';
import { DocumentWithMetadata } from '../../types/document.types';

@Injectable()
export class DocumentOwnershipService {
    constructor(private readonly documentsRepository: DocumentsRepository) {}

    async getOwnedDocument(
        documentId: string,
        userId: string,
    ): Promise<DocumentWithMetadata> {
        const document = await this.documentsRepository.findByIdAndUserId(
            documentId,
            userId,
        );

        if (!document) {
            throw new NotFoundException('Document not found.');
        }

        return document;
    }
}
