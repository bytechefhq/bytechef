import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType} from '@/shared/stores/useApplicationInfoStore';

interface ShouldShowEmbeddedGettingStartedProps {
    completed?: boolean;
    edition?: EditionType;
    environmentId: number;
    isAdmin: boolean;
}

export default function shouldShowEmbeddedGettingStarted({
    completed,
    edition,
    environmentId,
    isAdmin,
}: ShouldShowEmbeddedGettingStartedProps): boolean {
    return edition === EditionType.EE && environmentId === DEVELOPMENT_ENVIRONMENT && isAdmin && completed === false;
}
