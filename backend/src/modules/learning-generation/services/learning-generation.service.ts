import {
    Injectable,
    NotFoundException,
    BadRequestException,
} from '@nestjs/common';
import { DocumentStatus } from '@prisma/client';

import { DocumentsService } from '../../documents/services/documents.service';
import { StudyItemsService } from '../../study-items/study-items.service';

import { QuestionGenerationService } from './question-generation.service';
import { GenerateQuestionsRequestFactory } from '../factories/generate-questions-request.factory';
import { GeneratedQuestionMapper } from '../mappers/generated-question.mapper';

import { StudyItemResponseDto } from '../../study-items/dto/study-item-response.dto';
import { DocumentWithMetadata } from '../../documents/types/document.types';

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
        
        const { document, file } = await this.documentsService.getFileForUser(documentId,userId,);

        this.ensureReady(document);

        const request = this.requestFactory.fromDocument(document, file);

        const result = await this.questionGenerationService.generate(request);

        const commands = this.mapper.toCommands(userId, result.questions);

        return this.studyItemsService.createMany(commands);
    }
}
