import { UseGuards } from '@nestjs/common';
import { Controller, Param, Post } from '@nestjs/common';
import { CurrentUser } from '../../../../common/decorators/current-user.decorator';
import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';
import { LearningGenerationService } from '../../../learning-generation/application/services/learning-generation.service';

@Controller('documents')
@UseGuards(JwtAuthGuard)
export class DocumentCapabilitiesController {
    constructor(
        private readonly learningGenerationService: LearningGenerationService,
    ) {}

    @Post(':documentId/questions')
    generateQuestions(
        @CurrentUser('userId') userId: string,
        @Param('documentId') documentId: string,
    ) {
        return this.learningGenerationService.generateQuestions(
            userId,
            documentId,
        );
    }
}
