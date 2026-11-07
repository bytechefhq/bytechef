import {Node, useNodesInitialized, useReactFlow, useStore, useStoreApi} from '@xyflow/react';
import {useEffect, useRef} from 'react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import getInitialViewportPosition from '../utils/getInitialViewportPosition';
import getResizedCanvasViewportY from '../utils/getResizedCanvasViewportY';

export const CANVAS_TOP_INSET = 120;

export const CANVAS_LEFT_INSET = 80;

const PLACEMENT_NODE_TYPES = ['clusterRoot', 'graphFrame', 'readonly', 'triggerPlaceholder', 'workflow'];

const getPlacementNodes = (nodes: Node[]) =>
    nodes.filter((node) => !node.parentId && PLACEMENT_NODE_TYPES.includes(node.type ?? ''));

interface UseInitialViewportProps {
    enabled: boolean;
    getViewportOffsetX: () => number;
    workflowId?: string;
}

export default function useInitialViewport({enabled, getViewportOffsetX, workflowId}: UseInitialViewportProps) {
    const viewportPlacedRef = useRef(false);

    const placedWorkflowIdRef = useRef<string | undefined>(undefined);

    const previousCanvasHeightRef = useRef(0);

    const flowHeight = useStore((state) => state.height);
    const nodesInitialized = useNodesInitialized();
    const storeApi = useStoreApi();
    const {getNodes, getNodesBounds, getViewport, setViewport} = useReactFlow();

    useEffect(() => {
        if (!enabled || !workflowId || !nodesInitialized || placedWorkflowIdRef.current === workflowId) {
            return;
        }

        const placementNodes = getPlacementNodes(getNodes());

        if (placementNodes.length === 0) {
            return;
        }

        placedWorkflowIdRef.current = workflowId;

        const {x, y} = getInitialViewportPosition({
            canvasHeight: storeApi.getState().height,
            layoutDirection: useLayoutDirectionStore.getState().layoutDirection,
            leftInset: CANVAS_LEFT_INSET,
            nodesBounds: getNodesBounds(placementNodes),
            offsetX: getViewportOffsetX(),
            topInset: CANVAS_TOP_INSET,
        });

        void setViewport({x, y, zoom: 1}, {duration: viewportPlacedRef.current ? 500 : 0});

        viewportPlacedRef.current = true;
    }, [enabled, getNodes, getNodesBounds, getViewportOffsetX, nodesInitialized, setViewport, storeApi, workflowId]);

    useEffect(() => {
        const previousCanvasHeight = previousCanvasHeightRef.current;

        previousCanvasHeightRef.current = flowHeight;

        if (
            !enabled ||
            !previousCanvasHeight ||
            !flowHeight ||
            previousCanvasHeight === flowHeight ||
            !viewportPlacedRef.current ||
            useLayoutDirectionStore.getState().layoutDirection !== 'LR'
        ) {
            return;
        }

        const placementNodes = getPlacementNodes(getNodes());

        if (placementNodes.length === 0) {
            return;
        }

        const {x, y, zoom} = getViewport();

        void setViewport({
            x,
            y: getResizedCanvasViewportY({
                canvasHeight: flowHeight,
                nodesBounds: getNodesBounds(placementNodes),
                previousCanvasHeight,
                topInset: CANVAS_TOP_INSET,
                viewportY: y,
                zoom,
            }),
            zoom,
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [flowHeight]);
}
