import {useEmbeddedGettingStartedCompletedQuery} from '@/shared/middleware/graphql';
import {useGettingStartedStore} from '@/shared/stores/useGettingStartedStore';
import {useEffect} from 'react';

export default function useEmbeddedGettingStartedCompleted(enabled: boolean): boolean | undefined {
    const completeGettingStarted = useGettingStartedStore((state) => state.complete);
    const stored = useGettingStartedStore((state) => !!state.completions.embedded);

    const {data} = useEmbeddedGettingStartedCompletedQuery(undefined, {enabled: enabled && !stored});

    const serverCompleted = data?.embeddedGettingStartedCompleted;

    useEffect(() => {
        if (serverCompleted) {
            completeGettingStarted('embedded', 'COMPLETED');
        }
    }, [completeGettingStarted, serverCompleted]);

    return stored || serverCompleted;
}
