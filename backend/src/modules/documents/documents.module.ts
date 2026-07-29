import { DocumentsRepository } from './repositories/documents.repository';
import { Module } from '@nestjs/common';
import { DocumentsService } from './services/documents.service';
import { DocumentAnalysisModule } from '../document-analysis/document-analysis.module';
import { DocumentsController } from './controllers/documents.controller';
// import { FilesModule } from 'src/common/files/files.module';
import { DocumentUploadService } from './application/services/document-upload.service';
import { StorageModule } from '../storage/storage.module';
import { DocumentMapper } from './mappers/document.mapper';
import { FilesModule } from '../../common/files/files.module';
import { DocumentOwnershipService } from './application/services/document-ownership.service';

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
