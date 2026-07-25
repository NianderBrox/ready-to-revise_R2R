import { Body, Controller, Post } from '@nestjs/common';

import { MetadataService } from '../services/metadata.service';
import { GenerateMetadataDto } from '../dto/generate-metadata.dto';
import { GenerateMetadataRequest } from '../models/generate-metadata.request';

@Controller('ai')
export class AiController {
    constructor(private readonly metadataService: MetadataService) {}

    @Post('metadata')
    async generateMetadata(
        @Body()
        dto: GenerateMetadataDto,
    ) {
        return this.metadataService.generateMetadata(
            new GenerateMetadataRequest(dto.text),
        );
    }
}
