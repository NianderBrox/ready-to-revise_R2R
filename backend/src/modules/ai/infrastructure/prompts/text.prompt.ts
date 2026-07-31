import { Prompt } from './prompt.interface';

export abstract class TextPrompt implements Prompt<string> {
    abstract build(): string;

    parse(output: string): string {
        return output;
    }
}
