import { Injectable } from '@nestjs/common';

import { FileContent } from '../../../common/files/value-objects/file-content';

import { GenerateQuestionsRequest } from '../models/generate-questions.request';
import { DocumentWithMetadata } from '../../documents/types/document.types';

@Injectable()
export class GenerateQuestionsRequestFactory {
    fromDocument(
        document: DocumentWithMetadata,
        file: FileContent,
    ): GenerateQuestionsRequest {
        const context = [
            document.metadata!.subject,
            document.metadata!.chapter,
            document.metadata!.topic,
        ]
            .filter(Boolean)
            .join('\n');

        return new GenerateQuestionsRequest(
            file,
            document.title ?? undefined,
            context || undefined,
        );
    }
}
