import {useReactFlow, useStore} from '@xyflow/react';
import {useEffect, useRef} from 'react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import getInitialViewportPosition from '../utils/getInitialViewportPosition';

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
    const positionedWorkflowUuidRef = useRef<string | null>(null);

    const flowHeight = useStore((state) => state.height);
    const {setViewport} = useReactFlow();

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
}
