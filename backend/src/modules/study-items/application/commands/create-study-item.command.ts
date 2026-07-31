import { Difficulty, StudyItemType } from '../../../../common/enums';

export class CreateStudyItemCommand {
    userId!: string;

    title?: string;

    content?: string;

    type!: StudyItemType;

    difficulty?: Difficulty;

    topicId?: string;
}
