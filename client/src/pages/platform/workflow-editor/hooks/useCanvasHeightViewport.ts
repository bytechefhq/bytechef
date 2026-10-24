import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useReactFlow, useStore, useStoreApi} from '@xyflow/react';
import {useEffect, useRef} from 'react';

interface UseCanvasHeightViewportProps {
    enabled: boolean;
}

export default function useCanvasHeightViewport({enabled}: UseCanvasHeightViewportProps) {
    const previousHeightRef = useRef<number | undefined>(undefined);

    const clusterElementsCanvasOpen = useWorkflowEditorStore((state) => state.clusterElementsCanvasOpen);
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const height = useStore((state) => state.height);
    const storeApi = useStoreApi();
    const {setViewport} = useReactFlow();

    useEffect(() => {
        if (!height) {
            return;
        }

        const previousHeight = previousHeightRef.current;

        previousHeightRef.current = height;

        if (
            !enabled ||
            clusterElementsCanvasOpen ||
            layoutDirection !== 'LR' ||
            previousHeight === undefined ||
            previousHeight === height
        ) {
            return;
        }

        const [x, y, zoom] = storeApi.getState().transform;

        void setViewport({x, y: y + (height - previousHeight) / 2, zoom});
    }, [clusterElementsCanvasOpen, enabled, height, layoutDirection, setViewport, storeApi]);
}
