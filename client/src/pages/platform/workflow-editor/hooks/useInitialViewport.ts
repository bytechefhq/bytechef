import {useNodesInitialized, useReactFlow, useStore} from '@xyflow/react';
import {useEffect, useMemo, useRef} from 'react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getInitialViewportPosition from '../utils/getInitialViewportPosition';
import {
    INITIAL_FIT_VIEW_OPTIONS,
    getTopLevelTaskNames,
    isCrossAxisOverflowing,
    isWorkflowLaidOut,
} from '../utils/initialFitViewportUtils';

interface UseInitialViewportProps {
    canvasHeight: number;
    enabled: boolean;
    getViewportOffsetX: () => number;
    workflowUuid?: string;
}

export default function useInitialViewport({
    canvasHeight,
    enabled,
    getViewportOffsetX,
    workflowUuid,
}: UseInitialViewportProps) {
    const fittedWorkflowUuidRef = useRef<string | null>(null);
    const positionedWorkflowUuidRef = useRef<string | null>(null);

    const nodes = useWorkflowDataStore((state) => state.nodes);
    const workflowDefinition = useWorkflowDataStore((state) => state.workflow.definition);

    const flowHeight = useStore((state) => state.height);
    const flowWidth = useStore((state) => state.width);
    const nodesInitialized = useNodesInitialized();
    const {fitView, getNodesBounds, getViewport, setViewport} = useReactFlow();

    const topLevelTaskNames = useMemo(() => getTopLevelTaskNames(workflowDefinition), [workflowDefinition]);

    useEffect(() => {
        const workflowKey = workflowUuid ?? '';

        if (!enabled || !flowHeight || positionedWorkflowUuidRef.current === workflowKey) {
            return;
        }

        positionedWorkflowUuidRef.current = workflowKey;

        const {x, y} = getInitialViewportPosition({
            canvasHeight,
            flowHeight,
            layoutDirection: useLayoutDirectionStore.getState().layoutDirection,
            offsetX: getViewportOffsetX(),
        });

        void setViewport({x, y, zoom: 1});
    }, [canvasHeight, enabled, flowHeight, getViewportOffsetX, setViewport, workflowUuid]);

    useEffect(() => {
        const workflowKey = workflowUuid ?? '';

        if (
            !enabled ||
            !flowHeight ||
            !flowWidth ||
            !nodesInitialized ||
            positionedWorkflowUuidRef.current !== workflowKey ||
            fittedWorkflowUuidRef.current === workflowKey ||
            !isWorkflowLaidOut(nodes, topLevelTaskNames)
        ) {
            return;
        }

        fittedWorkflowUuidRef.current = workflowKey;

        const overflowing = isCrossAxisOverflowing({
            bounds: getNodesBounds(nodes),
            flowHeight,
            flowWidth,
            layoutDirection: useLayoutDirectionStore.getState().layoutDirection,
            viewport: getViewport(),
        });

        if (overflowing) {
            void fitView(INITIAL_FIT_VIEW_OPTIONS);
        }
    }, [
        enabled,
        fitView,
        flowHeight,
        flowWidth,
        getNodesBounds,
        getViewport,
        nodes,
        nodesInitialized,
        topLevelTaskNames,
        workflowUuid,
    ]);
}
