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
    AiModule
  ],
})
export class AppModule {}