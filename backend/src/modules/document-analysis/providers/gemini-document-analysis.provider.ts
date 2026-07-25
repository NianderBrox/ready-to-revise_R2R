import { GeminiClient } from "src/modules/ai/clients/gemini.client";
import { DocumentAnalysisProvider } from "../interfaces/document-analysis.provider";
import { AnalyzeDocumentRequest } from "../models/analyze-document.request";
import { DocumentAnalysisResult } from "../models/document-analysis.result";
import { Injectable } from "@nestjs/common";

@Injectable()
export class GeminiDocumentAnalysisProvider
  implements DocumentAnalysisProvider
{
  constructor(
    private readonly client: GeminiClient,
  ) {}

  async analyze(
    request: AnalyzeDocumentRequest,
  ): Promise<DocumentAnalysisResult> {
    throw new Error('Not implemented.');
  }
}