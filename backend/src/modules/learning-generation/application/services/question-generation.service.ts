import { Inject, Injectable } from '@nestjs/common';
import type { QuestionGenerationProvider } from '../../domain/interfaces/question-generation.provider';
import { GenerateQuestionsRequest } from '../../domain/models/generate-questions.request';
import { QuestionGenerationResult } from '../../domain/models/question-generation.result';
import { QUESTION_GENERATION_PROVIDER } from '../../infrastructure/tokens/question-generation-provider.token';

@Injectable()
export class QuestionGenerationService {
    constructor(
        @Inject(QUESTION_GENERATION_PROVIDER)
        private readonly provider: QuestionGenerationProvider,
    ) {}

    async generate(
        request: GenerateQuestionsRequest,
    ): Promise<QuestionGenerationResult> {
        return this.provider.generate(request);
    }
}
