import { Injectable } from '@nestjs/common';

import { FileContent } from '../../../../common/files/value-objects/file-content';

import { AnalyzeDocumentRequest } from '../../../document-analysis/models/analyze-document.request';
import { DocumentAnalysisService } from '../../../document-analysis/services/document-analysis.service';

import { CreateDocumentCommand } from '../../commands/create-document.command';
import { MarkDocumentAnalyzingCommand } from '../../commands/mark-document-analyzing.command';
import { MarkDocumentFailedCommand } from '../../commands/mark-document-failed.command';
import { MarkDocumentReadyCommand } from '../../commands/mark-document-ready.command';

import { CreateDocumentResponseDto } from '../../dto/create-document-response.dto';

import { DocumentsService } from '../../services/documents.service';
// import { AttachDocumentStorageCommand } from 'src/modules/storage/commands/attach-document-storage.command';
// import { SaveFileCommand } from 'src/modules/storage/commands/save-file.command';
import { StorageService } from '../../../storage/services/storage.service';
import { SaveFileCommand } from '../../../storage/commands/save-file.command';
import { AttachDocumentStorageCommand } from '../../../storage/commands/attach-document-storage.command';

@Injectable()
export class DocumentUploadService {
    constructor(
        private readonly documentsService: DocumentsService,

        private readonly storageService: StorageService,

        private readonly documentAnalysisService: DocumentAnalysisService,
    ) {}

    async upload(
        file: FileContent,
        command: CreateDocumentCommand,
    ): Promise<CreateDocumentResponseDto> {
        const document = await this.documentsService.create(command);

        const storageKey = await this.storageService.save(
            new SaveFileCommand({
                documentId: document.id,

                file: file.bytes,

                mimeType: file.mimeType,
            }),
        );

        await this.documentsService.attachStorage(
            document.id,

            new AttachDocumentStorageCommand({
                storageKey,

                fileSize: file.bytes.length,
            }),
        );

        await this.documentsService.markAnalyzing(
            document.id,

            new MarkDocumentAnalyzingCommand(),
        );

        try {
            const analysis = await this.documentAnalysisService.analyze(
                new AnalyzeDocumentRequest(file),
            );

            const updated = await this.documentsService.markReady(
                document.id,

                new MarkDocumentReadyCommand({
                    analysis,
                }),
            );

            return new CreateDocumentResponseDto(
                updated.id,

                updated.status,

                analysis,
            );
        } catch (error) {
            await this.documentsService.markFailed(
                document.id,

                new MarkDocumentFailedCommand({
                    reason:
                        error instanceof Error
                            ? error.message
                            : 'Unknown error',
                }),
            );

            throw error;
        }
    }
}
