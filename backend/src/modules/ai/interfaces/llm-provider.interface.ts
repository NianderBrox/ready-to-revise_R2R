import { Prompt } from '../prompts/prompt.interface';

export interface LlmProvider {
  generateText(
    prompt: Prompt<string>,
  ): Promise<string>;

  generateObject<T>(
    prompt: Prompt<T>,
  ): Promise<T>;
}