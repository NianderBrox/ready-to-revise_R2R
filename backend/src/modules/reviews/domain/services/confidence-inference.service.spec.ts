import {
    ConfidenceInferenceService,
    ConfidenceLevel,
} from './confidence-inference.service';

describe('ConfidenceInferenceService', () => {
    let service: ConfidenceInferenceService;

    beforeEach(() => {
        service = new ConfidenceInferenceService();
    });

    describe('perfect observables', () => {
        it('scores a fast, confident, correct answer as HIGH', () => {
            const result = service.infer({
                isCorrect: true,

                responseTimeMs: 2000,

                hesitationMs: 300,

                answerChanges: 0,
            });

            expect(result.level).toBe(ConfidenceLevel.HIGH);

            expect(result.score).toBeGreaterThan(0.75);
        });

        it('scores a slow, hesitant wrong answer as LOW', () => {
            const result = service.infer({
                isCorrect: false,

                responseTimeMs: 60000,

                hesitationMs: 15000,

                answerChanges: 3,
            });

            expect(result.level).toBe(ConfidenceLevel.LOW);
        });

        it('scores a fast clean wrong answer as MEDIUM', () => {
            const result = service.infer({
                isCorrect: false,

                responseTimeMs: 1000,

                hesitationMs: 200,

                answerChanges: 0,
            });

            expect(result.score).toBeCloseTo(
                0.25 * (1 - 1000 / 120000) +
                    0.2 * (1 - 200 / 30000) +
                    0.15 * 1.0,
                4,
            );

            expect(result.level).toBe(ConfidenceLevel.MEDIUM);
        });
    });

    describe('boundary thresholds', () => {
        it('worst-case correct observables land exactly on 0.40 and classify as MEDIUM', () => {
            const result = service.infer({
                isCorrect: true,

                responseTimeMs: 120000,

                hesitationMs: 30000,

                answerChanges: 5,
            });

            expect(result.score).toBeCloseTo(0.4, 4);

            expect(result.level).toBe(ConfidenceLevel.MEDIUM);
        });

        it('classifies scores below 0.40 as LOW', () => {
            const result = service.infer({
                isCorrect: false,

                responseTimeMs: 60000,

                hesitationMs: 15000,

                answerChanges: 3,
            });

            expect(result.score).toBeLessThan(0.4);

            expect(result.level).toBe(ConfidenceLevel.LOW);
        });
    });

    describe('missing telemetry neutral substitution', () => {
        it('treats missing timers as neutral so correct answers grade MEDIUM', () => {
            const result = service.infer({ isCorrect: true });

            expect(result.level).toBe(ConfidenceLevel.MEDIUM);
        });

        it('treats missing timers as neutral so wrong answers stay LOW', () => {
            const result = service.infer({ isCorrect: false });

            expect(result.level).toBe(ConfidenceLevel.LOW);
        });
    });

    describe('validation', () => {
        it.each([
            ['responseTimeMs', -1],
            ['hesitationMs', -5],
            ['answerChanges', -2],
        ] as const)('rejects negative %s', (field, value) => {
            expect(() =>
                service.infer({ isCorrect: true, [field]: value }),
            ).toThrow();
        });
    });
});
