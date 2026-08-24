import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { HealthModule } from './modules/health/health.module';
import { AuthModule } from './modules/auth/auth.module';
import { UsersModule } from './modules/users/users.module';
import { PrismaModule } from './prisma/prisma.module';
import { StudyItemsModule } from './modules/study-items/study-items.module';
import { SubjectsModule } from './modules/subject/subjects.module';
import { ChaptersModule } from './modules/chapters/chapters.module';
import { ReviewsModule } from './modules/reviews/reviews.module';
import { AttachmentsModule } from './modules/attachments/attachments.module';
import { DashboardModule } from './modules/dashboard/dashboard.module';
import { AiModule } from './modules/ai/ai.module';
import { DocumentAnalysisModule } from './modules/document-analysis/document-analysis.module';
import { LearningGenerationModule } from './modules/learning-generation/learning-generation.module';
import { DocumentsModule } from './modules/documents/documents.module';
import { MlClientModule } from './modules/ml-client/ml-client.module';
import { RecallPredictionsModule } from './modules/recall-predictions/recall-predictions.module';

@Module({
    imports: [
        ConfigModule.forRoot({
            isGlobal: true,
        }),
        HealthModule,
        AuthModule,
        UsersModule,
        PrismaModule,
        StudyItemsModule,
        SubjectsModule,
        ChaptersModule,
        ReviewsModule,
        AttachmentsModule,
        DashboardModule,
        AiModule,
        DocumentAnalysisModule,
        LearningGenerationModule,
        DocumentsModule,
        MlClientModule,
        RecallPredictionsModule,
    ],
})
export class AppModule {}
