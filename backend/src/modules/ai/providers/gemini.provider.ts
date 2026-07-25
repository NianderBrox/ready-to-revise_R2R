import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

import { GeminiClient } from '../clients/gemini.client';
import { LlmProvider } from '../interfaces/llm-provider.interface';
import { Prompt } from '../prompts/prompt.interface';
import { JsonParser } from '../utils/json-parser';

@Injectable()
export class GeminiProvider implements LlmProvider {
  private readonly model: string;

  constructor(
    private readonly client: GeminiClient,
    private readonly config: ConfigService,
  ) {
    this.model = this.config.getOrThrow<string>(
      'GEMINI_MODEL',
    );
  }

  private async invokeModel(
    prompt: string,
  ) {
    const ai = await this.client.getClient();

    return ai.interactions.create({
      model: this.model,
      input: prompt,
    });
  }

  private async getOutputText(
    prompt: string,
  ): Promise<string> {
    const interaction =
      await this.invokeModel(prompt);

    if (!interaction.output_text) {
      throw new Error(
        'Gemini returned an empty response.',
      );
    }

    return interaction.output_text;
  }

  async generateText(
    prompt: Prompt<string>,
  ): Promise<string> {
    return this.getOutputText(
      prompt.build(),
    );
  }

  async generateObject<T>(
    prompt: Prompt<T>,
  ): Promise<T> {
    const output =
      await this.getOutputText(
        prompt.build(),
      );

    return JsonParser.parse<T>(output);
  }
}