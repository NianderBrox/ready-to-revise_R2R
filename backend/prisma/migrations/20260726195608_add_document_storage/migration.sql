/*
  Warnings:

  - Added the required column `fileSize` to the `Document` table without a default value. This is not possible if the table is not empty.
  - Added the required column `storageKey` to the `Document` table without a default value. This is not possible if the table is not empty.

*/
-- AlterTable
ALTER TABLE "Document" ADD COLUMN     "checksum" TEXT,
ADD COLUMN     "fileSize" INTEGER NOT NULL,
ADD COLUMN     "storageKey" TEXT NOT NULL;
