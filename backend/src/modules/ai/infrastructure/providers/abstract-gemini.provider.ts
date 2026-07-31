import { GeminiClient } from '../clients/gemini.client';
import { EmptyAiResponseException } from '../../domain/exceptions/empty-ai-response.exception';
import { JsonParser } from '../utils/json-parser';
import { InvalidAiResponseException } from '../../domain/exceptions/invalid-ai-response.exception';

export abstract class AbstractGeminiProvider<TInput, TRequest extends object> {
    constructor(protected readonly client: GeminiClient) {}

    protected abstract buildInteractionRequest(input: TInput): TRequest;

    protected async execute(input: TInput): Promise<string> {
        const ai = await this.client.getClient();

        const interaction = await ai.interactions.create(
            this.buildInteractionRequest(input),
        );

        if (!interaction.output_text) {
            throw new EmptyAiResponseException();
        }

        return interaction.output_text;
    }

    protected async executeAndParse<TResult>(input: TInput): Promise<TResult> {
        const output = await this.execute(input);

        try {
            return JsonParser.parse<TResult>(output);
        } catch {
            throw new InvalidAiResponseException();
        }
    }
}
