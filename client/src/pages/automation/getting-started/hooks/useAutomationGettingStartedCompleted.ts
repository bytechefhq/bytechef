import {useAutomationGettingStartedCompletedQuery} from '@/shared/middleware/graphql';
import {useGettingStartedStore} from '@/shared/stores/useGettingStartedStore';
import {useEffect} from 'react';

interface UseAutomationGettingStartedCompletedProps {
    enabled: boolean;
    workspaceId: number;
}

export default function useAutomationGettingStartedCompleted({
    enabled,
    workspaceId,
}: UseAutomationGettingStartedCompletedProps): boolean | undefined {
    const completeGettingStarted = useGettingStartedStore((state) => state.complete);
    const stored = useGettingStartedStore((state) => !!state.completions.automation);

    const {data} = useAutomationGettingStartedCompletedQuery({workspaceId}, {enabled: enabled && !stored});

    const serverCompleted = data?.automationGettingStartedCompleted;

    useEffect(() => {
        if (serverCompleted) {
            completeGettingStarted('automation', 'COMPLETED');
        }
    }, [completeGettingStarted, serverCompleted]);

    return stored || serverCompleted;
}
