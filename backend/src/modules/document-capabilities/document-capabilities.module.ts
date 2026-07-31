import { Module } from '@nestjs/common';
import { DocumentsModule } from '../documents/documents.module';
import { LearningGenerationModule } from '../learning-generation/learning-generation.module';
import { DocumentCapabilitiesController } from './presentation/controllers/document-capabilities.controller';

@Module({
    imports: [DocumentsModule, LearningGenerationModule],
    controllers: [DocumentCapabilitiesController],
})
export class DocumentCapabilitiesModule {}
