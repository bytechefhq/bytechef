import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';

export function resolvePlatformMode(pathname: string, storedType: PlatformType | undefined): PlatformType {
    if (pathname.startsWith('/automation/') || pathname === '/automation') {
        return PlatformType.AUTOMATION;
    }

    if (pathname.startsWith('/embedded/') || pathname === '/embedded') {
        return PlatformType.EMBEDDED;
    }

    return storedType ?? PlatformType.AUTOMATION;
}
