import { Injectable } from '@nestjs/common';
import { FileContent } from '../../../../common/files/value-objects/file-content';
import { CreateDocumentCommand } from '../commands/create-document.command';
import { MarkDocumentReadyCommand } from '../commands/mark-document-ready.command';
import { CreateDocumentResponseDto } from '../../presentation/dto/create-document-response.dto';
import { DocumentsService } from './documents.service';
import { SaveFileCommand } from '../../../storage/application/commands/save-file.command';
import { AttachDocumentStorageCommand } from '../../../storage/application/commands/attach-document-storage.command';
import { DocumentAnalysisService } from '../../../document-analysis/application/services/document-analysis.service';
import { AnalyzeDocumentRequest } from '../../../document-analysis/domain/models/analyze-document.request';
import { StorageService } from '../../../storage/application/services/storage.service';

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

        await this.documentsService.markAnalyzing(document.id);

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
            await this.documentsService.markFailed(document.id);

            throw error;
        }
    }
}
