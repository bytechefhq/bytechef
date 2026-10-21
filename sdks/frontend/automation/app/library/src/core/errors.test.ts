import {describe, expect, it} from 'vitest';

import {BytechefAppError, isSignInRequired} from './errors';

describe('BytechefAppError', () => {
    it('carries code, status, runId and cause and is an Error', () => {
        const cause = new Error('socket');
        const error = new BytechefAppError('NETWORK_ERROR', 'fetch failed', {cause, runId: 'r-1'});

        expect(error).toBeInstanceOf(Error);
        expect(error.name).toBe('BytechefAppError');
        expect(error.code).toBe('NETWORK_ERROR');
        expect(error.status).toBeUndefined();
        expect(error.runId).toBe('r-1');
        expect(error.cause).toBe(cause);
        expect(error.message).toBe('fetch failed');
    });

    it('tells which errors end the user session', () => {
        expect(isSignInRequired(new BytechefAppError('APP_USER_REQUIRED', 'm'))).toBe(true);
        expect(isSignInRequired(new BytechefAppError('APP_USER_TOKEN_INVALID', 'm'))).toBe(true);
        expect(isSignInRequired(new BytechefAppError('SESSION_ENDED', 'm'))).toBe(true);
        expect(isSignInRequired(new BytechefAppError('APP_USER_FORBIDDEN', 'm'))).toBe(false);
        expect(isSignInRequired(new Error('APP_USER_REQUIRED'))).toBe(false);
    });
});
