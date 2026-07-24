import { PartialType } from '@nestjs/swagger';
import { CreateStudyItemDto } from './create-study-item.dto';

export class UpdateStudyItemDto extends PartialType(CreateStudyItemDto) {}