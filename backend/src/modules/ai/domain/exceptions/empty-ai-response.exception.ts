import { AiProviderException } from './ai-provider.exception';

export class EmptyAiResponseException extends AiProviderException {
    constructor() {
        super('The AI provider returned an empty response.');

        this.name = EmptyAiResponseException.name;
    }
}
