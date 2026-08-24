import { Injectable } from '@nestjs/common';
import { DocumentMetadata } from '@prisma/client';

@Injectable()
export class GenerateQuestionsPrompt {
    build(metadata: DocumentMetadata): string {
        const keywords = Array.isArray(metadata.keywords)
            ? metadata.keywords
                  .filter(
                      (keyword): keyword is string =>
                          typeof keyword === 'string',
                  )
                  .join(', ')
            : 'None';

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
- Every question must be answerable STRICTLY from content visible in the
  document. If a topic is not present or unclear, skip it entirely rather
  than guessing.
- Tag every question with "origin": if the question (with its options) exists
  verbatim or near-verbatim in the source document (e.g. it was an actual MCQ
  on the page), use "EXTRACTED"; if you authored the question yourself from
  the material, use "GENERATED". When ambiguous, choose "GENERATED".
- Every question MUST be multiple-choice with EXACTLY 4 options.
- Exactly ONE option is correct; "correctAnswerIndex" points to it (0-based).
- Distractors must be plausible, same topic, clearly wrong on careful reading.
- Do not use options like "All of the above" or "None of the above".
- Difficulty must be one of: EASY, MEDIUM, HARD.
- Include a short explanation whenever it improves understanding.
- If the source material contains diagrams, charts or figures, write questions
  that reference the shown visual (e.g. "In the diagram...", "According to the
  chart..."). The original picture is displayed with the question, so never
  re-describe the whole image inside the question text — ask about it instead.
- If a diagram question cannot have meaningful text options, fall back to
  numbered options ("1", "2", "3", "4") pointing at labelled parts of the
  visual rather than inventing unrelated text.

Return ONLY valid JSON.

Schema:

{
    "questions": [
        {
            "question": "string",
            "options": ["string", "string", "string", "string"],
            "correctAnswerIndex": 0,
            "answer": "the correct option text",
            "difficulty": "EASY | MEDIUM | HARD",
            "origin": "EXTRACTED | GENERATED",
            "explanation": "string"
        }
    ]
}

Document Metadata:

Subject:
${metadata.subject ?? 'Unknown'}

Chapter:
${metadata.chapter ?? 'Unknown'}

Topic:
${metadata.topic ?? 'Unknown'}

Difficulty:
${metadata.difficulty ?? 'Unknown'}

Keywords:
${keywords}
`;
    }
}
