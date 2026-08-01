import { FileContent } from '../../../../common/files/value-objects/file-content';
import { DocumentWithMetadata } from '../../../documents/domain/types/document.types';

export class GenerateQuestionsRequest {
    constructor(
        public readonly file: FileContent,
        public readonly metadata: NonNullable<DocumentWithMetadata['metadata']>,
    ) {}
}
