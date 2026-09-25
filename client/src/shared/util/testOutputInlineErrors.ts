/*
 * The workflow editor's Output tab renders a failed Test / Reset in place (see useOutputTab), so the
 * fetch interceptors skip their global toast for these requests. The REST pattern matches the node
 * test-output resource itself (PUT = test, DELETE = reset) but not `/exists` or `/sample-output`,
 * whose failures still toast.
 */
const TEST_OUTPUT_URL_PATTERN = /\/workflow-nodes\/[^/]+\/test-outputs(\?|$)/;

const INLINE_GRAPHQL_FIELDS = new Set(['saveClusterElementTestOutput']);

export function isInlineTestOutputUrl(url: string): boolean {
    return TEST_OUTPUT_URL_PATTERN.test(url);
}

export function isInlineTestOutputGraphQlError(error: {path?: Array<string | number>}): boolean {
    const field = error.path?.[0];

    return typeof field === 'string' && INLINE_GRAPHQL_FIELDS.has(field);
}
