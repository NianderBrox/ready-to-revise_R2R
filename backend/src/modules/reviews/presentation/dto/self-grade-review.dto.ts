import { IsInt, IsOptional, IsUUID, Min } from 'class-validator';

export class SelfGradeReviewDto {
    @IsUUID()
    studyItemId!: string;

    @IsOptional()
    @IsInt()
    @Min(0)
    responseTimeMs?: number;

    @IsOptional()
    @IsUUID()
    sessionId?: string;

    @IsOptional()
    @IsInt()
    @Min(0)
    sessionDurationMinutes?: number;

    @IsOptional()
    @IsInt()
    @Min(1)
    questionPositionInSession?: number;
}
