import {BASE_PATH} from '@/shared/middleware/platform/workflow/test';

export interface TestWorkflowRequestProps {
    inputs?: {[key: string]: object};
}

export interface GetTestWorkflowAttachRequestProps {
    jobId: string | number;
}

const getCookie = (name: string): string | undefined => {
    if (typeof document === 'undefined') {
        return undefined;
    }

    const escapeRegExp = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

    const match = document.cookie.match(new RegExp('(?:^|; )' + escapeRegExp(name) + '=([^;]*)'));

    return match ? decodeURIComponent(match[1]) : undefined;
};

/**
 * Constructs a GET request for the test workflow attach endpoint.
 *
 * @param {GetTestWorkflowAttachRequestProps} requestParameters - The parameters required to build the request.
 * @param {string} requestParameters.jobId - The unique identifier of the job.
 * @throws {Error} Throws an error if `jobId` is null or undefined.
 * @return {Object} An object containing `url` (string) for the API endpoint and `init` (RequestInit) for the request options.
 */
export function getTestWorkflowAttachRequest(requestParameters: GetTestWorkflowAttachRequestProps): {
    url: string;
    init: RequestInit;
} {
    if (requestParameters['jobId'] == null) {
        throw new Error(
            'Required parameter "jobId" was null or undefined when calling getTestWorkflowAttachRequest().'
        );
    }

    const url = `${BASE_PATH}/workflow-tests/${encodeURIComponent(String(requestParameters['jobId']))}/attach`;

    const headers: Record<string, string> = {
        Accept: 'text/event-stream',
    };

    const xsrf = getCookie('XSRF-TOKEN');
    if (xsrf) {
        headers['X-XSRF-TOKEN'] = xsrf;
    }

    const init: RequestInit = {
        credentials: 'include',
        headers,
        method: 'GET',
    };

    return {init, url};
}

export interface GetTestWorkflowStreamPostRequestProps {
    id: string;
    environmentId: number;
    testWorkflowRequest?: TestWorkflowRequestProps;
}

/**
 * Constructs a POST request for the test workflow stream endpoint.
 *
 * @param {GetTestWorkflowStreamPostRequestProps} requestParameters - The parameters required to build the request.
 * @param {string} requestParameters.id - The unique identifier of the workflow.
 * @param {number} requestParameters.environmentId - The environment identifier for the workflow test.
 * @param {object} [requestParameters.testWorkflowRequest] - Optional payload containing the test workflow request data.
 * @throws {Error} Throws an error if `id` or `environmentId` is null or undefined.
 * @return {Object} An object containing `url` (string) for the API endpoint and `init` (RequestInit) for the request options.
 */
export function getTestWorkflowStreamPostRequest(requestParameters: GetTestWorkflowStreamPostRequestProps): {
    url: string;
    init: RequestInit;
} {
    if (requestParameters['id'] == null) {
        throw new Error(
            'Required parameter "id" was null or undefined when calling getTestWorkflowStreamPostRequest().'
        );
    }

    if (requestParameters['environmentId'] == null) {
        throw new Error(
            'Required parameter "environmentId" was null or undefined when calling getTestWorkflowStreamPostRequest().'
        );
    }

    const url = `${BASE_PATH}/workflows/${encodeURIComponent(String(requestParameters['id']))}/tests?environmentId=${encodeURIComponent(String(requestParameters['environmentId']))}`;

    const headers: Record<string, string> = {
        Accept: 'text/event-stream',
        'Content-Type': 'application/json',
    };

    const xsrf = getCookie('XSRF-TOKEN');
    if (xsrf) {
        headers['X-XSRF-TOKEN'] = xsrf;
    }

    const body: BodyInit = JSON.stringify(requestParameters['testWorkflowRequest'] ?? {});

    const init: RequestInit = {
        body,
        credentials: 'include',
        headers,
        method: 'POST',
    };

    return {init, url};
}
