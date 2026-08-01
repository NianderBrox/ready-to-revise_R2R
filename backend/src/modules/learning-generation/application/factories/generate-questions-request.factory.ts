import { Injectable, BadRequestException } from '@nestjs/common';
import { FileContent } from '../../../../common/files/value-objects/file-content';
import { DocumentWithMetadata } from '../../../documents/domain/types/document.types';
import { GenerateQuestionsRequest } from '../../domain/models/generate-questions.request';

@Injectable()
export class GenerateQuestionsRequestFactory {
    fromDocument(
        document: DocumentWithMetadata,
        file: FileContent,
    ): GenerateQuestionsRequest {
        if (!document.metadata) {
            throw new BadRequestException(
                'Document metadata is not available.',
            );
        }

        return new GenerateQuestionsRequest(file, document.metadata);
    }
}
