import {BytechefAppError} from './errors';
import type {RequestResult} from './request';

function invalidResponse(status: number, path: string, expected: string): BytechefAppError {
    return new BytechefAppError(
        'INVALID_RESPONSE',
        `ByteChef answered ${status} to ${path} without ${expected}; check that BYTECHEF_URL points at the ByteChef server`,
        {status}
    );
}

export function expectArray<T>({data, status}: RequestResult<unknown>, path: string): T[] {
    if (!Array.isArray(data)) {
        throw invalidResponse(status, path, 'a JSON array');
    }

    return data as T[];
}

export function expectObject<T>({data, status}: RequestResult<unknown>, path: string): T {
    if (typeof data !== 'object' || data === null || Array.isArray(data)) {
        throw invalidResponse(status, path, 'a JSON object');
    }

    return data as T;
}
