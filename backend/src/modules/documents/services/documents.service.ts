import { Injectable } from '@nestjs/common';

import { DocumentStatus } from '@prisma/client';

import { DocumentAnalysisService } from '../../document-analysis/services/document-analysis.service';

import { CreateDocumentCommand } from '../commands/create-document.command';
import { MarkDocumentAnalyzingCommand } from '../commands/mark-document-analyzing.command';
import { MarkDocumentFailedCommand } from '../commands/mark-document-failed.command';
import { MarkDocumentReadyCommand } from '../commands/mark-document-ready.command';

import { DocumentsRepository } from '../repositories/documents.repository';

@Injectable()
export class DocumentsService {
    constructor(
        private readonly documentsRepository: DocumentsRepository,

        private readonly documentAnalysisService: DocumentAnalysisService,
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
        return this.documentsRepository.update(id, {
            title: command.title,
            status: DocumentStatus.READY,
        });
    }

    markFailed(id: string, command: MarkDocumentFailedCommand) {
        return this.documentsRepository.update(id, {
            status: DocumentStatus.FAILED,
        });
    }
}
