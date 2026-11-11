const HTTP_CLIENT_COMPONENT_NAME = 'httpClient';

interface BuildFirstCallCurlProps {
    apiBaseUrl: string;
    componentName?: string;
    componentVersion?: number;
    environmentName: string;
    externalUserId?: string;
}

export default function buildFirstCallCurl({
    apiBaseUrl,
    componentName,
    componentVersion,
    environmentName,
    externalUserId,
}: BuildFirstCallCurlProps): string {
    const httpClient = componentName === HTTP_CLIENT_COMPONENT_NAME;

    const actionName = httpClient ? 'get' : 'ACTION_NAME';
    const input = httpClient ? '{"uri": "https://httpbin.org/get"}' : '{}';

    const url = [
        apiBaseUrl,
        externalUserId ? encodeURIComponent(externalUserId) : 'EXTERNAL_USER_ID',
        'components',
        componentName || 'COMPONENT_NAME',
        'versions',
        componentVersion || 1,
        'actions',
        actionName,
    ].join('/');

    return [
        `curl -X POST ${quoteShellArgument(url)} \\`,
        '  -H "Authorization: Bearer $BYTECHEF_API_KEY" \\',
        `  -H 'X-Environment: ${environmentName}' \\`,
        "  -H 'Content-Type: application/json' \\",
        `  -d '{"input": ${input}}'`,
    ].join('\n');
}

function quoteShellArgument(value: string): string {
    return `'${value.replaceAll("'", `'\\''`)}'`;
}
