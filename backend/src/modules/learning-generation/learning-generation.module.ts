import { Module } from '@nestjs/common';

import { LearningGenerationService } from './services/learning-generation.service';
import { GeminiQuestionGenerationProvider } from './providers/gemini-question-generation.provider';
import { QUESTION_GENERATION_PROVIDER } from './tokens/question-generation-provider.token';
import { GenerateQuestionsPrompt } from './prompts/generate-questions.prompt';
import { StudyItemsModule } from '../study-items/study-items.module';
import { DocumentsModule } from '../documents/documents.module';
import { AiModule } from '../ai/ai.module';
import { GenerateQuestionsRequestFactory } from './factories/generate-questions-request.factory';
import { GeneratedQuestionMapper } from './mappers/generated-question.mapper';
import { QuestionGenerationService } from './services/question-generation.service';

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
