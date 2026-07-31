import { Injectable } from '@nestjs/common';
import { JsonParser } from '../../../ai/infrastructure/utils/json-parser';
import { MultimodalPrompt } from '../../../ai/infrastructure/prompts/multimodal-prompt.interface';
import { DocumentAnalysisResult } from '../../domain/models/document-analysis.result';

@Injectable()
export class AnalyzeDocumentPrompt implements MultimodalPrompt<DocumentAnalysisResult> {
    parse(output: string): DocumentAnalysisResult {
        return JsonParser.parse<DocumentAnalysisResult>(output);
    }

    instruction(): string {
        return `
You are an expert educational content analyzer.

Analyze the provided educational document.

Your tasks are:

1. Extract ALL readable text while preserving the original reading order.
2. Do NOT summarize during text extraction.
3. Preserve headings whenever possible.
4. Preserve numbered and bulleted lists.
5. Preserve mathematical equations as plain text.
6. Ignore page numbers, headers, and footers if they are not part of the educational content.
7. Return ONLY valid JSON.
8. Do NOT wrap the JSON inside markdown.
9. Do NOT include explanations.
10. Never invent information.

Return a single valid JSON object matching exactly this schema:

{
  "extractedText": string,
  "title": string,
  "summary": string,
  "subject": string,
  "chapter": string,
  "topic": string,
  "difficulty": "EASY" | "MEDIUM" | "HARD",
  "keywords": string[]
}

Rules:

- extractedText must contain all readable educational content.
- title should be concise.
- summary should be 2-3 sentences.
- subject should be the academic subject.
- chapter should be the chapter name.
- topic should be the most specific topic discussed.
- difficulty must be EASY, MEDIUM, or HARD.
- keywords should contain between 5 and 10 important concepts.

If a value cannot be determined confidently:

- use an empty string ("") for string fields
- use an empty array ([]) for keywords
- use "MEDIUM" for difficulty

Never fabricate information that is not present in the document.

Return ONLY valid JSON.
`;
    }
}
