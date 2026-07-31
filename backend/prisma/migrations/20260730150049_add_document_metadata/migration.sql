-- CreateTable
CREATE TABLE "DocumentMetadata" (
    "documentId" TEXT NOT NULL,
    "summary" TEXT NOT NULL,
    "subject" TEXT NOT NULL,
    "chapter" TEXT NOT NULL,
    "topic" TEXT NOT NULL,
    "difficulty" "Difficulty" NOT NULL,
    "keywords" JSONB NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "DocumentMetadata_pkey" PRIMARY KEY ("documentId")
);

-- AddForeignKey
ALTER TABLE "DocumentMetadata" ADD CONSTRAINT "DocumentMetadata_documentId_fkey" FOREIGN KEY ("documentId") REFERENCES "Document"("id") ON DELETE CASCADE ON UPDATE CASCADE;
