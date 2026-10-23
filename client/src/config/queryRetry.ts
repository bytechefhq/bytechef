const DEFAULT_MAX_QUERY_RETRIES = 3;

const REFUSED_GRAPHQL_CLASSIFICATIONS = ['FORBIDDEN', 'UNAUTHORIZED'];

const REFUSED_HTTP_STATUSES = [401, 403];

interface ErrorWithResponseI {
    response?: {status?: unknown};
}

interface ErrorWithGraphQlDetailsI {
    classification?: unknown;
    status?: unknown;
}

export const isRefusedRequestError = (error: unknown): boolean => {
    if (error === null || typeof error !== 'object') {
        return false;
    }

    const responseStatus = (error as ErrorWithResponseI).response?.status;

    if (typeof responseStatus === 'number' && REFUSED_HTTP_STATUSES.includes(responseStatus)) {
        return true;
    }

    const {classification, status} = error as ErrorWithGraphQlDetailsI;

    if (typeof status === 'number' && REFUSED_HTTP_STATUSES.includes(status)) {
        return true;
    }

    return typeof classification === 'string' && REFUSED_GRAPHQL_CLASSIFICATIONS.includes(classification);
};

export const shouldRetryQuery = (failureCount: number, error: unknown): boolean =>
    !isRefusedRequestError(error) && failureCount < DEFAULT_MAX_QUERY_RETRIES;
