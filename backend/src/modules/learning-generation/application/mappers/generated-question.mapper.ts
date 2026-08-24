import { BadRequestException, Injectable } from '@nestjs/common';
import { StudyItemType } from '../../../../common/enums';
import { CreateStudyItemCommand } from '../../../study-items/application/commands/create-study-item.command';
import { GeneratedQuestion } from '../../domain/models/generated-questions';

const MIN_OPTIONS = 2;

const MAX_OPTIONS = 6;

@Injectable()
export class GeneratedQuestionMapper {
    toCommand(
        userId: string,
        question: GeneratedQuestion,
        mediaDocumentId?: string,
    ): CreateStudyItemCommand {
        this.ensureValidMcq(question);

        return {
            userId,

            title: question.question,

            content:
                question.answer ??
                question.options[question.correctAnswerIndex],

            type: StudyItemType.QUESTION,

            difficulty: question.difficulty,

            options: question.options,

            correctAnswerIndex: question.correctAnswerIndex,

            origin: question.origin,

            mediaDocumentId,
        };
    }

    toCommands(
        userId: string,
        questions: GeneratedQuestion[],
        mediaDocumentId?: string,
    ): CreateStudyItemCommand[] {
        return questions.map((question) =>
            this.toCommand(userId, question, mediaDocumentId),
        );
    }

    private ensureValidMcq(question: GeneratedQuestion): void {
        if (
            !question ||
            typeof question.question !== 'string' ||
            question.question.trim().length === 0
        ) {
            throw new BadRequestException(
                'Generated question text is missing.',
            );
        }

        if (
            !Array.isArray(question.options) ||
            question.options.length < MIN_OPTIONS ||
            question.options.length > MAX_OPTIONS ||
            question.options.some(
                (option) => typeof option !== 'string' || option.length === 0,
            )
        ) {
            throw new BadRequestException(
                'Generated question must contain 2-6 non-empty options.',
            );
        }

        if (
            !Number.isInteger(question.correctAnswerIndex) ||
            question.correctAnswerIndex < 0 ||
            question.correctAnswerIndex >= question.options.length
        ) {
            throw new BadRequestException(
                'correctAnswerIndex must point to one of the options.',
            );
        }

        if (
            question.difficulty !== undefined &&
            !['EASY', 'MEDIUM', 'HARD'].includes(question.difficulty)
        ) {
            throw new BadRequestException(
                'Generated difficulty must be EASY, MEDIUM or HARD.',
            );
        }

        if (
            question.origin !== undefined &&
            question.origin !== 'EXTRACTED' &&
            question.origin !== 'GENERATED'
        ) {
            throw new BadRequestException(
                'Generated origin must be EXTRACTED or GENERATED.',
            );
        }
    }
}
