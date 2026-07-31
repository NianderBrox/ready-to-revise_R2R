import { Injectable } from '@nestjs/common';
import { Document } from '@prisma/client';
import { DocumentDetailsResponseDto } from '../../presentation/dto/document-details.response.dto';
import { DocumentSummaryResponseDto } from '../../presentation/dto/document-summary.dto';

@Injectable()
export class DocumentMapper {
    toSummaryDto(document: Document): DocumentSummaryResponseDto {
        return new DocumentSummaryResponseDto(
            document.id,

            document.title,

            document.originalName,

            document.mimeType,

            document.status,

            document.createdAt,
        );
    }

    toDetailsDto(document: Document): DocumentDetailsResponseDto {
        return new DocumentDetailsResponseDto(
            document.id,

            document.title,

            document.originalName,

            document.mimeType,

            document.status,

            document.createdAt,

            document.updatedAt,
        );
    }
}
