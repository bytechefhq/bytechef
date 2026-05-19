import {DEFAULT_CANVAS_WIDTH} from '@/shared/constants';
import {useEffect, useRef, useState} from 'react';

const useWorkflowExecutionSheetWorkflowPanel = () => {
    const [canvasWidth, setCanvasWidth] = useState(DEFAULT_CANVAS_WIDTH);

    const rootDivRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!rootDivRef.current) {
            return;
        }

        const updateWidth = () => {
            if (rootDivRef.current) {
                setCanvasWidth(rootDivRef.current.clientWidth);
            }
        };

        updateWidth();

        const resizeObserver = new ResizeObserver(updateWidth);

        resizeObserver.observe(rootDivRef.current);

        return () => resizeObserver.disconnect();
    }, []);

    return {
        canvasWidth,
        rootDivRef,
    };
};

export default useWorkflowExecutionSheetWorkflowPanel;
