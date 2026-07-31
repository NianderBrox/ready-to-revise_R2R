import {
    Controller,
    Param,
    Post,
} from '@nestjs/common';

import { JwtAuthGuard } from '../../auth/guards/jwt-auth.guard';

import { LearningGenerationService } from '../../learning-generation/services/learning-generation.service';

import { UseGuards } from '@nestjs/common';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';

@Controller('documents')
@UseGuards(JwtAuthGuard)
export class DocumentCapabilitiesController {
    constructor(
        private readonly learningGenerationService: LearningGenerationService,
    ) {}

    @Post(':documentId/questions')
    generateQuestions(
        @CurrentUser('id') userId: string,
        @Param('documentId') documentId: string,
    ) {
        return this.learningGenerationService.generateQuestions(
            userId,
            documentId,
        );
    }
}