import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {resolveShellPlatformMode} from '@/shared/navigation/platformMode';
import {EditionType} from '@/shared/stores/useApplicationInfoStore';

interface ShouldShowEmbeddedGettingStartedHelpLinkProps {
    edition?: EditionType;
    environmentId: number;
    isAdmin: boolean;
    pathname: string;
    storedPlatformType?: PlatformType;
}

export default function shouldShowEmbeddedGettingStartedHelpLink({
    edition,
    environmentId,
    isAdmin,
    pathname,
    storedPlatformType,
}: ShouldShowEmbeddedGettingStartedHelpLinkProps): boolean {
    return (
        edition === EditionType.EE &&
        environmentId === DEVELOPMENT_ENVIRONMENT &&
        isAdmin &&
        resolveShellPlatformMode(pathname, storedPlatformType) === PlatformType.EMBEDDED
    );
}
