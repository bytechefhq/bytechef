import {PlatformType} from '@/pages/home/stores/usePlatformTypeStore';
import {resolveShellPlatformMode} from '@/shared/navigation/platformMode';

interface ShouldShowAutomationGettingStartedHelpLinkProps {
    canCreateProject: boolean;
    development: boolean;
    pathname: string;
    storedPlatformType?: PlatformType;
}

export default function shouldShowAutomationGettingStartedHelpLink({
    canCreateProject,
    development,
    pathname,
    storedPlatformType,
}: ShouldShowAutomationGettingStartedHelpLinkProps): boolean {
    return (
        canCreateProject &&
        development &&
        resolveShellPlatformMode(pathname, storedPlatformType) === PlatformType.AUTOMATION
    );
}
