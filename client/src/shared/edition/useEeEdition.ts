import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';

export default function useEeEdition(): boolean {
    const application = useApplicationInfoStore((state) => state.application);

    return application?.edition === EditionType.EE;
}
