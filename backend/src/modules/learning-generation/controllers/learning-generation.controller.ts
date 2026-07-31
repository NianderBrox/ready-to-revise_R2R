import { Controller, Param, Post, UseGuards } from '@nestjs/common';

import { JwtAuthGuard } from '../../auth/guards/jwt-auth.guard';

import { LearningGenerationService } from '../services/learning-generation.service';
import { CurrentUser } from '../../../common/decorators/current-user.decorator';


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
