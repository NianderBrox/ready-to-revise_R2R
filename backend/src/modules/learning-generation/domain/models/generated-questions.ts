import { Difficulty } from '../../../../common/enums';

export class GeneratedQuestion {
    question!: string;

    options!: string[];

    correctAnswerIndex!: number;

    answer?: string;

    difficulty!: Difficulty;

    origin?: 'EXTRACTED' | 'GENERATED';

    explanation?: string;
}
