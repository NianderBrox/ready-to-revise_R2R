import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { HttpModule } from '@nestjs/axios';

import { MlConfigService } from './application/services/ml-config.service';
import { MlHttpService } from './infrastructure/http/ml-http.service';

@Module({
    imports: [ConfigModule, HttpModule.register({})],
    providers: [MlConfigService, MlHttpService],
    exports: [MlHttpService, MlConfigService],
})
export class MlClientModule {}
