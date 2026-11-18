import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {ORGANIZATION_SETTINGS_PATH} from '@/shared/navigation/settingsPaths';

const getUrlPlatformMode = (pathname: string): PlatformType | undefined => {
    if (pathname.startsWith('/automation/') || pathname === '/automation') {
        return PlatformType.AUTOMATION;
    }

    if (pathname.startsWith('/embedded/') || pathname === '/embedded') {
        return PlatformType.EMBEDDED;
    }

    return undefined;
};

export function resolvePlatformMode(pathname: string, storedType: PlatformType | undefined): PlatformType {
    return getUrlPlatformMode(pathname) ?? storedType ?? PlatformType.AUTOMATION;
}

export function resolveShellPlatformMode(
    pathname: string,
    storedType: PlatformType | undefined
): PlatformType | undefined {
    const isOrganizationSettingsPath =
        pathname === ORGANIZATION_SETTINGS_PATH || pathname.startsWith(`${ORGANIZATION_SETTINGS_PATH}/`);

    if (isOrganizationSettingsPath) {
        return resolvePlatformMode(pathname, storedType);
    }

    return getUrlPlatformMode(pathname);
}
