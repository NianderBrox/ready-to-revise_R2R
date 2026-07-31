import { Injectable, NotFoundException } from '@nestjs/common';

import { DocumentStatus } from '@prisma/client';

import { DocumentAnalysisService } from '../../document-analysis/services/document-analysis.service';

import { CreateDocumentCommand } from '../commands/create-document.command';
import { MarkDocumentAnalyzingCommand } from '../commands/mark-document-analyzing.command';
import { MarkDocumentFailedCommand } from '../commands/mark-document-failed.command';
import { MarkDocumentReadyCommand } from '../commands/mark-document-ready.command';

import { DocumentsRepository } from '../repositories/documents.repository';
// import { AttachDocumentStorageCommand } from 'src/modules/storage/commands/attach-document-storage.command';
import { DocumentSummaryResponseDto } from '../dto/document-summary.dto';
import { DocumentMapper } from '../mappers/document.mapper';
import { AttachDocumentStorageCommand } from '../../storage/commands/attach-document-storage.command';
import { DocumentDetailsResponseDto } from '../dto/document-details.response.dto';
import { DocumentOwnershipService } from '../application/services/document-ownership.service';
import { StorageService } from '../../storage/services/storage.service';
import { Document } from '@prisma/client';
import { Difficulty } from '@prisma/client';
import { DocumentWithMetadata } from '../types/document.types';
import { FileContent } from '../../../common/files/value-objects/file-content';

@Injectable()
export class DocumentsService {
    constructor(
        private readonly documentsRepository: DocumentsRepository,

        private readonly documentOwnershipService: DocumentOwnershipService,

        private readonly storageService: StorageService,

        private readonly documentMapper: DocumentMapper,
    ) {}

    create(command: CreateDocumentCommand) {
        return this.documentsRepository.create({
            userId: command.userId,
            originalName: command.originalName,
            mimeType: command.mimeType,
            status: DocumentStatus.UPLOADED,
        });
    }

    findById(id: string) {
        return this.documentsRepository.findById(id);
    }

    markAnalyzing(id: string, command: MarkDocumentAnalyzingCommand) {
        return this.documentsRepository.update(id, {
            status: DocumentStatus.ANALYZING,
        });
    }

    markReady(id: string, command: MarkDocumentReadyCommand) {
        return this.documentsRepository.markReady(id, command.analysis);
    }

    markFailed(id: string, command: MarkDocumentFailedCommand) {
        return this.documentsRepository.update(id, {
            status: DocumentStatus.FAILED,
        });
    }

    async attachStorage(id: string, command: AttachDocumentStorageCommand) {
        return this.documentsRepository.attachStorage(id, {
            storageKey: command.storageKey,

            fileSize: command.fileSize,

            checksum: command.checksum,
        });
    }

    async listForUser(userId: string): Promise<DocumentSummaryResponseDto[]> {
        const documents =
            await this.documentsRepository.findManyByUserId(userId);

        return documents.map((document) =>
            this.documentMapper.toSummaryDto(document),
        );
    }

    async findByIdForUser(
        documentId: string,
        userId: string,
    ): Promise<DocumentDetailsResponseDto> {
        const document = await this.documentOwnershipService.getOwnedDocument(
            documentId,
            userId,
        );

        return this.documentMapper.toDetailsDto(document);
    }

    async getFileForUser(
        documentId: string,
        userId: string,
    ): Promise<{
        document: DocumentWithMetadata;
        file: FileContent;
    }> {
        const document = await this.documentOwnershipService.getOwnedDocument(
            documentId,
            userId,
        );

        const bytes = await this.storageService.read(document.storageKey!);

        const file = new FileContent(
            bytes,
            document.mimeType,
            document.originalName,
        );

        return {
            document,
            file,
        };
    }
}
