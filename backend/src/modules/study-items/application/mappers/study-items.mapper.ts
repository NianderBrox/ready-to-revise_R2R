import { StudyItem } from '@prisma/client';
import { StudyItemResponseDto } from '../../presentation/dto/study-item-response.dto';

export class StudyItemsMapper {
    static toResponse(studyItem: StudyItem): StudyItemResponseDto {
        return {
            id: studyItem.id,
            title: studyItem.title ?? undefined,
            content: studyItem.content ?? undefined,
            type: studyItem.type,
            difficulty: studyItem.difficulty ?? undefined,
            topicId: studyItem.topicId ?? undefined,
            options: this.toStringArray(studyItem.options),
            origin: studyItem.origin ?? undefined,
            mediaDocumentId: studyItem.mediaDocumentId ?? undefined,
            nextReviewAt: studyItem.nextReviewAt ?? undefined,
            createdAt: studyItem.createdAt,
            updatedAt: studyItem.updatedAt,
        };
    }

    private static toStringArray(value: unknown): string[] | undefined {
        if (!Array.isArray(value)) {
            return undefined;
        }

        return value.filter(
            (entry): entry is string => typeof entry === 'string',
        );
    }
}
