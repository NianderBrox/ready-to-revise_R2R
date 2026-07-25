import {
    IsInt,
    IsOptional,
    IsString,
    IsUrl,
    IsUUID,
    Min,
    MaxLength,
} from 'class-validator';

export class CreateAttachmentDto {
    @IsUrl()
    url!: string;

    @IsOptional()
    @IsString()
    mimeType?: string;

    @IsOptional()
    @IsInt()
    @Min(0)
    fileSize?: number;

    @IsUUID()
    studyItemId!: string;

    @IsOptional()
    @IsString()
    @MaxLength(50)
    storageProvider?: string;
}
