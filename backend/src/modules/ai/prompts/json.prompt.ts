import { Prompt } from './prompt.interface';
import { JsonParser } from '../utils/json-parser';

export abstract class JsonPrompt<T> implements Prompt<T> {
    abstract build(): string;

    parse(output: string): T {
        return JsonParser.parse<T>(output);
    }
}
