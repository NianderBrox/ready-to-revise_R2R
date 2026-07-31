import { Controller, Get } from '@nestjs/common';
import { HealthService } from '../../application/services/health.service';

@Controller('health')
export class HealthController {
    constructor(private readonly healthService: HealthService) {}

    @Get()
    check() {
        return this.healthService.check();
    }
}
