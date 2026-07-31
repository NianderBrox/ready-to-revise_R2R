import { Injectable } from '@nestjs/common';

@Injectable()
export class GenerateQuestionsPrompt {
    build(title?: string, context?: string): string {
        return `
You are an expert educator.

Your task is to generate high-quality canonical revision questions from the ATTACHED study material.

The study material is attached as a document. Carefully analyze the entire document before generating questions.

Requirements:

- Cover all important concepts.
- Generate questions that test conceptual understanding rather than memorization.
- Avoid duplicate or overlapping questions.
- Keep every question independent.
- Use only information present in the attached document.
- Do not invent facts.
- Answers should be concise and accurate.
- Difficulty must be one of: EASY, MEDIUM, HARD.
- Include a short explanation whenever it improves understanding.

Return ONLY valid JSON.

Schema:

{
  "questions": [
    {
      "question": "string",
      "answer": "string",
      "difficulty": "EASY | MEDIUM | HARD",
      "explanation": "string"
    }
  ]
}

Document Title:
${title ?? 'Unknown'}

Additional Context:
${context ?? 'None'}
`;
    }
}
