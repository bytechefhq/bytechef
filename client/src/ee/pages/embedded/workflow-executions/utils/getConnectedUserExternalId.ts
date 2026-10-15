const EMBEDDED_DEPLOYMENT_PREFIX = '__EMBEDDED__';

const ENVIRONMENT_SUFFIX_PATTERN = /__(DEVELOPMENT|STAGING|PRODUCTION)$/;

export function getConnectedUserExternalId(deploymentName?: string): string | undefined {
    if (!deploymentName) {
        return undefined;
    }

    const withoutPrefix = deploymentName.startsWith(EMBEDDED_DEPLOYMENT_PREFIX)
        ? deploymentName.substring(EMBEDDED_DEPLOYMENT_PREFIX.length)
        : deploymentName;

    return withoutPrefix.replace(ENVIRONMENT_SUFFIX_PATTERN, '');
}
