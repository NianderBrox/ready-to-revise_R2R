import { Controller, Param, Post, UseGuards } from '@nestjs/common';
import { CurrentUser } from '../../../../common/decorators/current-user.decorator';
import { JwtAuthGuard } from '../../../auth/infrastructure/guards/jwt-auth.guard';
import { LearningGenerationService } from '../../application/services/learning-generation.service';
import type { CurrentUserData } from '../../../../common/interfaces/current-user-data.interface';

@Controller('documents')
@UseGuards(JwtAuthGuard)
export class LearningGenerationController {
    constructor(
        private readonly learningGenerationService: LearningGenerationService,
    ) {}

    @Post(':documentId/questions')
    generateQuestions(
        @CurrentUser() user: CurrentUserData,
        @Param('documentId') documentId: string,
    ) {
        return this.learningGenerationService.generateQuestions(
            user.userId,
            documentId,
        );
    }
}
