import { Prompt } from '../../infrastructure/prompts/prompt.interface';

export interface LlmProvider {
    generate<T>(prompt: Prompt<T>): Promise<T>;
}
