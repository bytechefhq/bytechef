import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType} from '@/shared/stores/useApplicationInfoStore';

interface ShouldShowEmbeddedGettingStartedHelpLinkProps {
    edition?: EditionType;
    environmentId: number;
    isAdmin: boolean;
    pathname: string;
}

export default function shouldShowEmbeddedGettingStartedHelpLink({
    edition,
    environmentId,
    isAdmin,
    pathname,
}: ShouldShowEmbeddedGettingStartedHelpLinkProps): boolean {
    return (
        edition === EditionType.EE &&
        environmentId === DEVELOPMENT_ENVIRONMENT &&
        isAdmin &&
        pathname.startsWith('/embedded/')
    );
}
