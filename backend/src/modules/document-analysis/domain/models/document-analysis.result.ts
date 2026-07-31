import { Difficulty } from '@prisma/client';

export class DocumentAnalysisResult {
    constructor(
        public readonly extractedText: string,
        public readonly title: string,
        public readonly summary: string,
        public readonly subject: string,
        public readonly chapter: string,
        public readonly topic: string,
        public readonly difficulty: Difficulty,
        public readonly keywords: string[],
    ) {}
}
