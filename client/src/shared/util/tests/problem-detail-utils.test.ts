import {describe, expect, it, vi} from 'vitest';

import {isAccessDeniedProblemDetail, readProblemDetail} from '../problem-detail-utils';

function createResponse(json: () => Promise<unknown>): Response {
    return {
        clone: vi.fn(() => ({json: vi.fn(json)})),
    } as unknown as Response;
}

describe('problem-detail-utils', () => {
    describe('readProblemDetail', () => {
        it('reads the body from a clone of the response', async () => {
            const response = createResponse(() => Promise.resolve({errorCode: 'ACCESS_DENIED', title: 'Forbidden'}));

            const problemDetail = await readProblemDetail(response);

            expect(problemDetail).toEqual({errorCode: 'ACCESS_DENIED', title: 'Forbidden'});
            expect(response.clone).toHaveBeenCalledTimes(1);
        });

        it('returns null when the body is not JSON', async () => {
            const response = createResponse(() => Promise.reject(new Error('Not JSON')));

            expect(await readProblemDetail(response)).toBeNull();
        });

        it('returns null when the response cannot be cloned', async () => {
            expect(await readProblemDetail({} as Response)).toBeNull();
        });
    });

    describe('isAccessDeniedProblemDetail', () => {
        it('is true only for an ACCESS_DENIED error code', () => {
            expect(isAccessDeniedProblemDetail({errorCode: 'ACCESS_DENIED'})).toBe(true);
            expect(isAccessDeniedProblemDetail({errorCode: 'SOMETHING_ELSE'})).toBe(false);
            expect(isAccessDeniedProblemDetail({})).toBe(false);
            expect(isAccessDeniedProblemDetail(null)).toBe(false);
            expect(isAccessDeniedProblemDetail(undefined)).toBe(false);
        });
    });
});
