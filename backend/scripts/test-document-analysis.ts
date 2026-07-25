// import { NestFactory } from '@nestjs/core';

// import { AiTestingModule } from '../src/devtools/ai-testing.module';
// import { createAiContext } from 'src/devtools/create-ai-context';

// async function bootstrap() {

//   const app = await createAiContext();

//   console.log(
//     'Nest application created.',
//   );

//   await app.close();
// }

// bootstrap();


import 'reflect-metadata';
import { readFile } from 'node:fs/promises';
import { extname } from 'node:path';

import { NestFactory } from '@nestjs/core';

import { AiTestingModule } from '../src/devtools/ai-testing.module';

import { DocumentAnalysisService } from '../src/modules/document-analysis/services/document-analysis.service';

import { FileContent } from '../src/common/files/value-objects/file-content';
import { AnalyzeDocumentRequest } from '../src/modules/document-analysis/models/analyze-document.request';
import { basename } from 'node:path';

const MIME_TYPES: Record<string, string> = {
  '.pdf': 'application/pdf',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
};

async function main() {
  const app =
    await NestFactory.createApplicationContext(
      AiTestingModule,
    );

  try {
    const filePath =
      process.argv[2];

    if (!filePath) {
      throw new Error(
        'Usage: npm run ai:test <file>',
      );
    }

    const bytes =
      await readFile(filePath);

    const extension =
      extname(filePath).toLowerCase();

    const mimeType =
      MIME_TYPES[extension];

    if (!mimeType) {
      throw new Error(
        `Unsupported file type: ${extension}`,
      );
    }

    const file =
      new FileContent(
        bytes,
        mimeType,
        basename(filePath),
      );

    const request =
      new AnalyzeDocumentRequest(
        file,
      );

    const service =
      app.get(
        DocumentAnalysisService,
      );

    const result =
      await service.analyze(
        request,
      );

    console.log(
      JSON.stringify(
        result,
        null,
        2,
      ),
    );
  } finally {
    await app.close();
  }
}

main().catch(console.error);