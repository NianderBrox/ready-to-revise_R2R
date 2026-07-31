import { Module } from '@nestjs/common';
import { SubjectsController } from './presentation/controllers/subjects.controller';
import { SubjectsRepository } from './infrastructure/repositories/subjects.repository';
import { PrismaModule } from '../../prisma/prisma.module';
import { SubjectsService } from './application/services/subjects.service';

@Module({
    imports: [PrismaModule],
    controllers: [SubjectsController],
    providers: [SubjectsService, SubjectsRepository],
    exports: [SubjectsRepository],
})
export class SubjectsModule {}
