export class StudyItemResponseDto {
    id!: string;

    title?: string;

    content?: string;

    type!: string;

    difficulty?: string;

    topicId?: string;

    options?: string[];

    origin?: string;

    mediaDocumentId?: string;

    nextReviewAt?: Date;

    createdAt!: Date;

    updatedAt!: Date;
}
