export interface BytechefAppErrorOptions {
    cause?: unknown;
    runId?: string;
    status?: number;
}

export class BytechefAppError extends Error {
    readonly code: string;
    readonly runId?: string;
    readonly status?: number;

    constructor(code: string, message: string, options: BytechefAppErrorOptions = {}) {
        super(message, options.cause === undefined ? undefined : {cause: options.cause});

        this.name = 'BytechefAppError';
        this.code = code;
        this.status = options.status;
        this.runId = options.runId;
    }
}
