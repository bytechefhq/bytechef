const EMBEDDED_DEPLOYMENT_PREFIX = '__EMBEDDED__';

const ENVIRONMENT_SUFFIX_PATTERN = /__(DEVELOPMENT|STAGING|PRODUCTION)$/;

export function getConnectedUserExternalId(deploymentName?: string): string | undefined {
    if (!deploymentName) {
        return undefined;
    }

    if (!deploymentName.startsWith(EMBEDDED_DEPLOYMENT_PREFIX)) {
        return deploymentName;
    }

    return deploymentName.substring(EMBEDDED_DEPLOYMENT_PREFIX.length).replace(ENVIRONMENT_SUFFIX_PATTERN, '');
}
