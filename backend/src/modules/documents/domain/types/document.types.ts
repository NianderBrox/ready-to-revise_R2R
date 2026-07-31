import { Prisma } from '@prisma/client';

export type DocumentWithMetadata = Prisma.DocumentGetPayload<{
    include: {
        metadata: true;
    };
}>;
