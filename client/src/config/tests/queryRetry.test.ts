import {isRefusedRequestError, shouldRetryQuery} from '@/config/queryRetry';
import {ResponseError} from '@/shared/middleware/automation/configuration';
import {GraphQlRequestError} from '@/shared/middleware/graphqlFetcher';
import {describe, expect, it} from 'vitest';

const responseError = (status: number) =>
    new ResponseError(new Response(null, {status}), 'Response returned an error code');

describe('isRefusedRequestError', () => {
    it('recognises a REST ResponseError with status 401 or 403', () => {
        expect(isRefusedRequestError(responseError(401))).toBe(true);
        expect(isRefusedRequestError(responseError(403))).toBe(true);
    });

    it('does not treat other REST statuses as refused', () => {
        expect(isRefusedRequestError(responseError(404))).toBe(false);
        expect(isRefusedRequestError(responseError(500))).toBe(false);
    });

    it('recognises a GraphQL error refused at the HTTP level', () => {
        expect(isRefusedRequestError(new GraphQlRequestError('Forbidden', {status: 403}))).toBe(true);
        expect(isRefusedRequestError(new GraphQlRequestError('Unauthorized', {status: 401}))).toBe(true);
    });

    it('recognises a GraphQL error classified FORBIDDEN or UNAUTHORIZED', () => {
        expect(isRefusedRequestError(new GraphQlRequestError('Access denied', {classification: 'FORBIDDEN'}))).toBe(
            true
        );
        expect(
            isRefusedRequestError(new GraphQlRequestError('Authentication required', {classification: 'UNAUTHORIZED'}))
        ).toBe(true);
    });

    it('does not treat other GraphQL errors as refused', () => {
        expect(isRefusedRequestError(new GraphQlRequestError('Boom', {classification: 'INTERNAL_ERROR'}))).toBe(false);
        expect(isRefusedRequestError(new GraphQlRequestError('Boom', {status: 500}))).toBe(false);
    });

    it('does not treat plain errors or non-objects as refused', () => {
        expect(isRefusedRequestError(new Error('Network failure'))).toBe(false);
        expect(isRefusedRequestError(null)).toBe(false);
        expect(isRefusedRequestError(undefined)).toBe(false);
        expect(isRefusedRequestError('403')).toBe(false);
    });
});

describe('shouldRetryQuery', () => {
    it('never retries a refused request', () => {
        expect(shouldRetryQuery(0, responseError(403))).toBe(false);
        expect(shouldRetryQuery(0, responseError(401))).toBe(false);
        expect(shouldRetryQuery(0, new GraphQlRequestError('Access denied', {classification: 'FORBIDDEN'}))).toBe(
            false
        );
    });

    it('keeps the default of three retries for other errors', () => {
        const serverError = responseError(500);

        expect(shouldRetryQuery(0, serverError)).toBe(true);
        expect(shouldRetryQuery(2, serverError)).toBe(true);
        expect(shouldRetryQuery(3, serverError)).toBe(false);
        expect(shouldRetryQuery(0, new Error('Network failure'))).toBe(true);
    });
});
