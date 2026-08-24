-- Add raw observables + derived confidence to Review (D4/D11); MCQ fields and initial-due slot to StudyItem (D6/D10)

-- AlterTable
ALTER TABLE "StudyItem" ADD COLUMN     "correctAnswerIndex" INTEGER,
ADD COLUMN     "nextReviewAt" TIMESTAMP(3),
ADD COLUMN     "options" JSONB;

-- AlterTable
ALTER TABLE "Review" ADD COLUMN     "answerChanges" INTEGER,
ADD COLUMN     "confidenceScore" DOUBLE PRECISION,
ADD COLUMN     "hesitationMs" INTEGER,
ADD COLUMN     "isCorrect" BOOLEAN,
ADD COLUMN     "responseTimeMs" INTEGER,
ADD COLUMN     "selectedOptionIndex" INTEGER,
ADD COLUMN     "sessionId" TEXT;

-- CreateIndex
CREATE INDEX "StudyItem_type_nextReviewAt_idx" ON "StudyItem"("type", "nextReviewAt");

-- CreateIndex
CREATE INDEX "Review_sessionId_idx" ON "Review"("sessionId");
