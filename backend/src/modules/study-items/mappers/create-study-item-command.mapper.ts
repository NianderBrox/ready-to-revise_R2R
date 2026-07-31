import { CreateStudyItemDto } from '../dto/create-study-item.dto';
import { CreateStudyItemCommand } from '../commands/create-study-item.command';

export class CreateStudyItemCommandMapper {
    static fromDto(
        userId: string,
        dto: CreateStudyItemDto,
    ): CreateStudyItemCommand {
        return {
            userId,
            title: dto.title,
            content: dto.content,
            type: dto.type,
            difficulty: dto.difficulty,
            topicId: dto.topicId,
        };
    }
}
