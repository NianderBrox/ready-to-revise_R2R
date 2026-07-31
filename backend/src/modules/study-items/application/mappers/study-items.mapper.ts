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
            createdAt: studyItem.createdAt,
            updatedAt: studyItem.updatedAt,
        };
    }
}
