import { AiProviderException } from './ai-provider.exception';

export class InvalidAiResponseException extends AiProviderException {
    constructor() {
        super('The AI provider returned an invalid response.');

        this.name = InvalidAiResponseException.name;
    }
}
