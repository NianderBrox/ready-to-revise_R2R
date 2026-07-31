import { DocumentsRepository } from './infrastructure/repositories/documents.repository';
import { Module } from '@nestjs/common';
import { DocumentsService } from './application/services/documents.service';
import { DocumentAnalysisModule } from '../document-analysis/document-analysis.module';
import { DocumentUploadService } from './application/services/document-upload.service';
import { StorageModule } from '../storage/storage.module';
import { FilesModule } from '../../common/files/files.module';
import { DocumentOwnershipService } from './application/services/document-ownership.service';
import { DocumentMapper } from './application/mappers/document.mapper';
import { DocumentsController } from './presentation/controllers/documents.controller';

@Module({
    imports: [DocumentAnalysisModule, FilesModule, StorageModule],
    providers: [
        DocumentsRepository,

        DocumentsService,

        DocumentUploadService,

        DocumentOwnershipService,

        DocumentMapper,
    ],
    exports: [DocumentsRepository, DocumentsService, DocumentUploadService],
    controllers: [DocumentsController],
})
export class DocumentsModule {}
