import { DocumentsRepository } from './repositories/documents.repository';
import { Module } from '@nestjs/common';
import { DocumentsService } from './services/documents.service';
import { DocumentAnalysisModule } from '../document-analysis/document-analysis.module';
import { DocumentsController } from './controllers/documents.controller';
import { FilesModule } from 'src/common/files/files.module';
import { DocumentUploadService } from './application/services/document-upload.service';
@Module({
    imports: [DocumentAnalysisModule, FilesModule],
    providers: [
        // FileMapper,
        DocumentsRepository,
        DocumentsService,
    ],
    exports: [DocumentsRepository, DocumentsService, DocumentUploadService],
    controllers: [DocumentsController],
})
export class DocumentsModule {}
