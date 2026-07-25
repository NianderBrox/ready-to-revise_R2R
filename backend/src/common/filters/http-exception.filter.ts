import {
    ArgumentsHost,
    Catch,
    ExceptionFilter,
    HttpException,
    HttpStatus,
} from '@nestjs/common';

@Catch()
export class HttpExceptionFilter implements ExceptionFilter {
    catch(exception: unknown, host: ArgumentsHost) {
        const ctx = host.switchToHttp();

        const response = ctx.getResponse();

        let status = HttpStatus.INTERNAL_SERVER_ERROR;

        let message = 'Internal server error';

        let errors: string[] = [];

        if (exception instanceof HttpException) {
            status = exception.getStatus();

            const exceptionResponse = exception.getResponse();

            if (typeof exceptionResponse === 'string') {
                message = exceptionResponse;
            } else {
                const res = exceptionResponse as any;

                message = res.message || message;

                errors = Array.isArray(res.message) ? res.message : [];
            }
        }

        response.status(status).json({
            success: false,
            message,
            errors,
        });
    }
}
