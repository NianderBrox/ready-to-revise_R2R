import { Injectable } from '@nestjs/common';

import { StudyItemType } from '../../../common/enums';

import { CreateStudyItemCommand } from '../../study-items/commands/create-study-item.command';

import { GeneratedQuestion } from '../models/generated-questions';

@Injectable()
export class GeneratedQuestionMapper {
    toCommand(
        userId: string,
        question: GeneratedQuestion,
    ): CreateStudyItemCommand {
        return {
            userId,

            title: question.question,

            content: question.answer,

            type: StudyItemType.QUESTION,

            difficulty: question.difficulty,
        };
    }

    toCommands(
        userId: string,
        questions: GeneratedQuestion[],
    ): CreateStudyItemCommand[] {
        return questions.map((question) => this.toCommand(userId, question));
    }
}
