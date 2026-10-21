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

const SIGN_IN_REQUIRED_CODES: readonly string[] = ['APP_USER_REQUIRED', 'APP_USER_TOKEN_INVALID', 'SESSION_ENDED'];

/**
 * True when the error means the request has no usable user session: no user token where one is needed, a user token
 * ByteChef rejected, or a refresh the authorization server refused. The stored tokens are then worthless and the user
 * has to sign in again. `APP_USER_FORBIDDEN` is not one: signing in again does not grant access.
 */
export function isSignInRequired(error: unknown): boolean {
    return error instanceof BytechefAppError && SIGN_IN_REQUIRED_CODES.includes(error.code);
}
