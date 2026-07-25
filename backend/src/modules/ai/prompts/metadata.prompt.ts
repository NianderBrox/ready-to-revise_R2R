import { Prompt } from './prompt.interface';
import { GeneratedMetadataDto } from '../dto/generated-metadata.dto';

export class MetadataPrompt
  implements Prompt<GeneratedMetadataDto>
{
  constructor(
    private readonly text: string,
  ) {}

  build(): string {
    return `
You are an expert educational content analyzer.

Your task is to analyze the given study material and return ONLY valid JSON.

Do not wrap the JSON in markdown.
Do not explain anything.
Do not include comments.
Do not include extra text.

Return exactly this schema:

{
  "title": string,
  "summary": string,
  "subject": string,
  "chapter": string,
  "topic": string,
  "difficulty": "EASY" | "MEDIUM" | "HARD",
  "keywords": string[]
}

Rules:

- Title should be concise.
- Summary should be 2-3 sentences.
- Subject should be the academic subject.
- Chapter should be the chapter name.
- Topic should be the most specific topic.
- Difficulty should represent the learning difficulty.
- Keywords should contain 5-10 important concepts.
- Respond ONLY with JSON.

Study Material:

${this.text}
`;
  }
}