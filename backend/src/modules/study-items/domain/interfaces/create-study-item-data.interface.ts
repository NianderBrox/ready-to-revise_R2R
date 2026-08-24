import { Difficulty, StudyItemType } from '../../../../common/enums';

export interface CreateStudyItemData {
    title?: string;
    content?: string;
    type: StudyItemType;
    difficulty?: Difficulty;
    topicId?: string;
    userId: string;

    options?: string[];
    correctAnswerIndex?: number;
    origin?: string;
    mediaDocumentId?: string;
    nextReviewAt?: Date;
}
