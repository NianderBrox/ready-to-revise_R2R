export class MlUnavailableException extends Error {
    constructor(reason: string) {
        super(`ML service unavailable: ${reason}`);

        this.name = 'MlUnavailableException';
    }
}
