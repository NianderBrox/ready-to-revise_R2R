export class AiProviderException extends Error {
    constructor(message: string) {
        super(message);

        this.name = AiProviderException.name;
    }
}
