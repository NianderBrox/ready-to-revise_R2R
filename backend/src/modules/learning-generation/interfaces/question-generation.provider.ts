import { GenerateQuestionsRequest } from '../models/generate-questions.request';
import { QuestionGenerationResult } from '../models/question-generation.result';

export interface QuestionGenerationProvider {
    generate(
        request: GenerateQuestionsRequest,
    ): Promise<QuestionGenerationResult>;
}
