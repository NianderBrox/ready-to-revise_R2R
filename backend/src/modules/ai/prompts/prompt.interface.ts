export interface Prompt<T> {
    build(): string;

    parse(output: string): T;
}
