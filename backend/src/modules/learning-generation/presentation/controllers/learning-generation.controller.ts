import { Controller, Param, Post, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../../../../common/decorators/current-user.decorator';
import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';
import { LearningGenerationService } from '../../application/services/learning-generation.service';

@Controller('learning-generation')
@UseGuards(JwtAuthGuard)
export class LearningGenerationController {
    constructor(
        private readonly learningGenerationService: LearningGenerationService,
    ) {}

    @Post('documents/:documentId/questions')
    generateQuestions(
        @CurrentUser('id') userId: string,
        @Param('documentId') documentId: string,
    ) {
        console.log('Controller reached');
        return this.learningGenerationService.generateQuestions(
            userId,
            documentId,
        );
    }
}
