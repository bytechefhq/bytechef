const LEGACY_EMBEDDED_DEPLOYMENT_PREFIX = '__EMBEDDED__';

const LEGACY_ENVIRONMENT_SUFFIX_PATTERN = /__(DEVELOPMENT|STAGING|PRODUCTION)$/;

export function getConnectedUserExternalId(deploymentName?: string): string | undefined {
    if (!deploymentName) {
        return undefined;
    }

    if (!deploymentName.startsWith(LEGACY_EMBEDDED_DEPLOYMENT_PREFIX)) {
        return deploymentName;
    }

    return deploymentName
        .substring(LEGACY_EMBEDDED_DEPLOYMENT_PREFIX.length)
        .replace(LEGACY_ENVIRONMENT_SUFFIX_PATTERN, '');
}
