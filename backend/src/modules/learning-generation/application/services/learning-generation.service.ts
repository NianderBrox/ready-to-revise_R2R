import {
    Injectable,
    NotFoundException,
    BadRequestException,
} from '@nestjs/common';
import { DocumentStatus } from '@prisma/client';
import { QuestionGenerationService } from './question-generation.service';
import { GenerateQuestionsRequestFactory } from '../factories/generate-questions-request.factory';
import { GeneratedQuestionMapper } from '../mappers/generated-question.mapper';
import { DocumentsService } from '../../../documents/application/services/documents.service';
import { DocumentWithMetadata } from '../../../documents/domain/types/document.types';
import { StudyItemsService } from '../../../study-items/application/services/study-items.service';
import { StudyItemResponseDto } from '../../../study-items/presentation/dto/study-item-response.dto';

@Injectable()
export class LearningGenerationService {
    constructor(
        private readonly documentsService: DocumentsService,
        private readonly questionGenerationService: QuestionGenerationService,
        private readonly requestFactory: GenerateQuestionsRequestFactory,
        private readonly mapper: GeneratedQuestionMapper,
        private readonly studyItemsService: StudyItemsService,
    ) {}

    private ensureReady(document: DocumentWithMetadata): void {
        if (document.status !== DocumentStatus.READY) {
            throw new BadRequestException(
                'Document has not finished processing.',
            );
        }
    }

    async generateQuestions(
        userId: string,
        documentId: string,
    ): Promise<StudyItemResponseDto[]> {
        const { document, file } = await this.documentsService.getFileForUser(
            documentId,
            userId,
        );

        this.ensureReady(document);

        const request = this.requestFactory.fromDocument(document, file);

        const result = await this.questionGenerationService.generate(request);

        const isImageSource = document.mimeType
            ?.toLowerCase()
            .startsWith('image/');

        const commands = this.mapper.toCommands(
            userId,
            result.questions,
            isImageSource ? documentId : undefined,
        );

        return this.studyItemsService.createMany(commands);
    }
}
