import {useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useDisabledTaskNames from '../hooks/useDisabledTaskNames';
import useTakenEmptyConditionCases from '../hooks/useTakenEmptyConditionCases';
import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import resolveGhostBarSideStatuses, {GhostBarSideStatusesI} from './resolveGhostBarSideStatuses';
import useTaskDispatcherGhostStatus from './useTaskDispatcherGhostStatus';

/** Per-half executed status of a task dispatcher ghost bar. */
export default function useTaskDispatcherGhostBarStatuses(
    id: string,
    data: unknown,
    isBottomGhost: boolean
): GhostBarSideStatusesI {
    const {edges, nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
        }))
    );

    const disabledTaskNames = useDisabledTaskNames();
    const workflowTestNodeStates = useWorkflowTestNodeStates();

    const fallbackStatus = useTaskDispatcherGhostStatus(data);
    const takenEmptyConditionCases = useTakenEmptyConditionCases(
        Boolean((data as {conditionId?: string} | undefined)?.conditionId)
    );

    return useMemo(
        () =>
            resolveGhostBarSideStatuses({
                disabledTaskNames,
                edges,
                fallbackStatus,
                ghostNodeId: id,
                isBottomGhost,
                nodes,
                takenEmptyConditionCases,
                workflowTestNodeStates,
            }),
        [
            disabledTaskNames,
            edges,
            fallbackStatus,
            id,
            isBottomGhost,
            nodes,
            takenEmptyConditionCases,
            workflowTestNodeStates,
        ]
    );
}
