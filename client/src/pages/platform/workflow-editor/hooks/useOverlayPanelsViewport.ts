import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useReactFlow, useStoreApi} from '@xyflow/react';
import {useCallback, useEffect, useRef} from 'react';

import useOverlayPanelsState from '../hooks/useOverlayPanelsState';
import {easeOutCubic} from '../utils/animateNodePositions';
import {computeOverlayViewportOffset} from '../utils/overlayPanelViewport';

export const OVERLAY_PAN_DURATION = 300;

interface UseOverlayPanelsViewportProps {
    enabled: boolean;
}

export default function useOverlayPanelsViewport({enabled}: UseOverlayPanelsViewportProps) {
    const appliedOffsetRef = useRef<number | undefined>(undefined);
    const pendingTargetXRef = useRef<number | undefined>(undefined);
    const pendingTimeoutRef = useRef<number | undefined>(undefined);

    const clusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.clusterElementsCanvasOpen);

    const storeApi = useStoreApi();
    const {setViewport} = useReactFlow();

    const {
        dataPillPanelOpen,
        issuesSidebarOpen,
        rightSidebarOpen,
        workflowNodeDetailsPanelOpen,
        workflowTestChatPanelOpen,
    } = useOverlayPanelsState();

    const getViewportOffsetX = useCallback(() => appliedOffsetRef.current ?? 0, []);

    useEffect(() => {
        if (!enabled || clusterElementsCanvasOpen) {
            return;
        }

        const [currentX, currentY, zoom] = storeApi.getState().transform;

        const offset = computeOverlayViewportOffset({
            dataPillPanelOpen,
            issuesSidebarOpen,
            rightSidebarOpen,
            workflowNodeDetailsPanelOpen,
            workflowTestChatPanelOpen,
        });
        const previousOffset = appliedOffsetRef.current;
        const baseX = pendingTargetXRef.current ?? currentX;
        const targetX = baseX - (previousOffset ?? 0) + offset;

        appliedOffsetRef.current = offset;

        if (targetX === baseX) {
            return;
        }

        const viewport = {x: targetX, y: currentY, zoom};

        if (previousOffset === undefined) {
            setViewport(viewport);

            return;
        }

        window.clearTimeout(pendingTimeoutRef.current);

        pendingTargetXRef.current = targetX;
        pendingTimeoutRef.current = window.setTimeout(() => {
            pendingTargetXRef.current = undefined;
        }, OVERLAY_PAN_DURATION);

        setViewport(viewport, {duration: OVERLAY_PAN_DURATION, ease: easeOutCubic, interpolate: 'linear'});
    }, [
        clusterElementsCanvasOpen,
        dataPillPanelOpen,
        enabled,
        issuesSidebarOpen,
        rightSidebarOpen,
        setViewport,
        storeApi,
        workflowNodeDetailsPanelOpen,
        workflowTestChatPanelOpen,
    ]);

    return {getViewportOffsetX};
}
