import { Difficulty } from '../../../common/enums';

export class GeneratedQuestion {
    question!: string;

    answer!: string;

    difficulty!: Difficulty;

    explanation?: string;
}
