import {useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

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

    const workflowTestNodeStates = useWorkflowTestNodeStates();

    const fallbackStatus = useTaskDispatcherGhostStatus(data);

    return useMemo(
        () =>
            resolveGhostBarSideStatuses({
                edges,
                fallbackStatus,
                ghostNodeId: id,
                isBottomGhost,
                nodes,
                workflowTestNodeStates,
            }),
        [edges, fallbackStatus, id, isBottomGhost, nodes, workflowTestNodeStates]
    );
}
