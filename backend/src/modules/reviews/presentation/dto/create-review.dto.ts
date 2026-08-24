import { IsInt, IsOptional, IsUUID, Max, Min } from 'class-validator';

export class CreateReviewDto {
    @IsUUID()
    studyItemId!: string;

    @IsInt()
    @Min(0)
    @Max(9)
    selectedOptionIndex!: number;

    @IsOptional()
    @IsInt()
    @Min(0)
    responseTimeMs?: number;

    @IsOptional()
    @IsInt()
    @Min(0)
    hesitationMs?: number;

    @IsOptional()
    @IsInt()
    @Min(0)
    answerChanges?: number;

    @IsOptional()
    @IsUUID()
    sessionId?: string;
}
