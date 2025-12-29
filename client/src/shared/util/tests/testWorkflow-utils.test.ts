import {BASE_PATH} from '@/shared/middleware/platform/workflow/test';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {getTestWorkflowAttachRequest, getTestWorkflowStreamPostRequest} from '../testWorkflow-utils';

describe('testWorkflow-utils', () => {
    beforeEach(() => {
        vi.stubGlobal('document', {
            cookie: '',
        });
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    describe('getTestWorkflowAttachRequest', () => {
        it('should throw an error if jobId is missing', () => {
            // @ts-expect-error jobId is required
            expect(() => getTestWorkflowAttachRequest({})).toThrow(
                'Required parameter "jobId" was null or undefined when calling getTestWorkflowAttachRequest().'
            );
        });

        it('should return correct URL and init for a valid jobId', () => {
            const jobId = 'test-job-id';
            const result = getTestWorkflowAttachRequest({jobId});

            expect(result.url).toBe(`${BASE_PATH}/workflow-tests/${jobId}/attach`);
            expect(result.init.method).toBe('GET');
            expect(result.init.headers).toEqual({
                Accept: 'text/event-stream',
            });
            expect(result.init.credentials).toBe('include');
        });

        it('should encode jobId in the URL', () => {
            const jobId = 'job id / with special characters';
            const result = getTestWorkflowAttachRequest({jobId});

            expect(result.url).toBe(`${BASE_PATH}/workflow-tests/${encodeURIComponent(jobId)}/attach`);
        });

        it('should include X-XSRF-TOKEN header if XSRF-TOKEN cookie exists', () => {
            vi.stubGlobal('document', {
                cookie: 'XSRF-TOKEN=secret-token',
            });

            const result = getTestWorkflowAttachRequest({jobId: '123'});
            expect(result.init.headers).toHaveProperty('X-XSRF-TOKEN', 'secret-token');
        });

        it('should handle multiple cookies and extract XSRF-TOKEN', () => {
            vi.stubGlobal('document', {
                cookie: 'other-cookie=foo; XSRF-TOKEN=secret-token; another=bar',
            });

            const result = getTestWorkflowAttachRequest({jobId: '123'});
            expect(result.init.headers).toHaveProperty('X-XSRF-TOKEN', 'secret-token');
        });
    });

    describe('getTestWorkflowStreamPostRequest', () => {
        it('should throw an error if id is missing', () => {
            // @ts-expect-error id is required
            expect(() => getTestWorkflowStreamPostRequest({environmentId: 1})).toThrow(
                'Required parameter "id" was null or undefined when calling getTestWorkflowStreamPostRequest().'
            );
        });

        it('should throw an error if environmentId is missing', () => {
            // @ts-expect-error environmentId is required
            expect(() => getTestWorkflowStreamPostRequest({id: 'workflow-id'})).toThrow(
                'Required parameter "environmentId" was null or undefined when calling getTestWorkflowStreamPostRequest().'
            );
        });

        it('should return correct URL and init for valid parameters', () => {
            const id = 'workflow-123';
            const environmentId = 456;
            const result = getTestWorkflowStreamPostRequest({environmentId, id});

            expect(result.url).toBe(`${BASE_PATH}/workflows/${id}/tests?environmentId=${environmentId}`);
            expect(result.init.method).toBe('POST');
            expect(result.init.headers).toEqual({
                Accept: 'text/event-stream',
                'Content-Type': 'application/json',
            });
            expect(result.init.body).toBe(JSON.stringify({}));
            expect(result.init.credentials).toBe('include');
        });

        it('should encode id and environmentId in the URL', () => {
            const id = 'id with spaces';
            const environmentId = 789;
            const result = getTestWorkflowStreamPostRequest({environmentId, id});

            expect(result.url).toBe(`${BASE_PATH}/workflows/${encodeURIComponent(id)}/tests?environmentId=789`);
        });

        it('should include testWorkflowRequest in the body if provided', () => {
            const testWorkflowRequest = {inputs: {key: {}}};
            const result = getTestWorkflowStreamPostRequest({
                environmentId: 456,
                id: '123',
                testWorkflowRequest,
            });

            expect(result.init.body).toBe(JSON.stringify(testWorkflowRequest));
        });

        it('should include X-XSRF-TOKEN header if XSRF-TOKEN cookie exists', () => {
            vi.stubGlobal('document', {
                cookie: 'XSRF-TOKEN=secret-post-token',
            });

            const result = getTestWorkflowStreamPostRequest({environmentId: 456, id: '123'});
            expect(result.init.headers).toHaveProperty('X-XSRF-TOKEN', 'secret-post-token');
        });
    });

    describe('getCookie', () => {
        it('should return undefined if document is undefined', () => {
            vi.stubGlobal('document', undefined);

            const result = getTestWorkflowAttachRequest({jobId: '123'});
            expect(result.init.headers).not.toHaveProperty('X-XSRF-TOKEN');
        });

        it('should return undefined if cookie is not found', () => {
            vi.stubGlobal('document', {
                cookie: 'foo=bar',
            });
            const result = getTestWorkflowAttachRequest({jobId: '123'});
            expect(result.init.headers).not.toHaveProperty('X-XSRF-TOKEN');
        });

        it('should handle special characters in cookie name', () => {
            // This tests escapeRegExp inside getCookie
            vi.stubGlobal('document', {
                cookie: 'XSRF.TOKEN=special; XSRF-TOKEN=standard',
            });
            const result = getTestWorkflowAttachRequest({jobId: '123'});
            expect(result.init.headers).toHaveProperty('X-XSRF-TOKEN', 'standard');
        });
    });
});
