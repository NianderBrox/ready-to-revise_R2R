import { Module } from '@nestjs/common';
import { GeminiQuestionGenerationProvider } from './infrastructure/providers/gemini-question-generation.provider';
import { QUESTION_GENERATION_PROVIDER } from './infrastructure/tokens/question-generation-provider.token';
import { GenerateQuestionsPrompt } from './infrastructure/prompts/generate-questions.prompt';
import { StudyItemsModule } from '../study-items/study-items.module';
import { DocumentsModule } from '../documents/documents.module';
import { AiModule } from '../ai/ai.module';
import { GenerateQuestionsRequestFactory } from './application/factories/generate-questions-request.factory';
import { GeneratedQuestionMapper } from './application/mappers/generated-question.mapper';
import { LearningGenerationService } from './application/services/learning-generation.service';
import { QuestionGenerationService } from './application/services/question-generation.service';

@Module({
    imports: [AiModule, DocumentsModule, StudyItemsModule],
    providers: [
        QuestionGenerationService,
        LearningGenerationService,
        GenerateQuestionsPrompt,
        GenerateQuestionsRequestFactory,
        GeneratedQuestionMapper,
        {
            provide: QUESTION_GENERATION_PROVIDER,
            useClass: GeminiQuestionGenerationProvider,
        },
    ],
    exports: [LearningGenerationService],
})
export class LearningGenerationModule {}
