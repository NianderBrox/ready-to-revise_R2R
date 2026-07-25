import { Subject } from '@prisma/client';
import { SubjectResponseDto } from './dto/subject-response.dto';

export class SubjectsMapper {
    static toResponse(subject: Subject): SubjectResponseDto {
        return {
            id: subject.id,
            name: subject.name,
            createdAt: subject.createdAt,
            updatedAt: subject.updatedAt,
        };
    }
}
