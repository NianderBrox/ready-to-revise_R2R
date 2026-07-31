import { Inject, Injectable } from '@nestjs/common';

import { GenerateQuestionsRequest } from '../models/generate-questions.request';
import { QuestionGenerationResult } from '../models/question-generation.result';

import type { QuestionGenerationProvider } from '../interfaces/question-generation.provider';
import { QUESTION_GENERATION_PROVIDER } from '../tokens/question-generation-provider.token';

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
