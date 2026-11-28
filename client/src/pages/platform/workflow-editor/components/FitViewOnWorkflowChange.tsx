import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useNodesInitialized, useReactFlow, useStore} from '@xyflow/react';
import {useEffect} from 'react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {getAxisCenteredViewport} from '../utils/axisCenteredViewportUtils';

const FIT_VIEW_OPTIONS = {maxZoom: 1, minZoom: 0.2, padding: 0.2};

const TOP_OFFSET = 72;

const TRIGGER_NODE_HALF_WIDTH = 72 / 2;

const FitViewOnWorkflowChange = ({customCanvasWidth}: {customCanvasWidth?: number}) => {
    const workflowUuid = useWorkflowDataStore((state) => state.workflow.workflowUuid);
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const {fitView, getNodes, getNodesBounds, setViewport} = useReactFlow();
    const nodesInitialized = useNodesInitialized();
    const flowHeight = useStore((state) => state.height);
    const flowWidth = useStore((state) => state.width);

    useEffect(() => {
        if (!nodesInitialized) {
            return;
        }

        const nodes = getNodes();
        const triggerNode = nodes[0];

        if (layoutDirection === 'TB' && triggerNode && flowWidth && flowHeight > TOP_OFFSET) {
            const viewport = getAxisCenteredViewport({
                ...FIT_VIEW_OPTIONS,
                axisX: triggerNode.position.x + TRIGGER_NODE_HALF_WIDTH,
                bounds: getNodesBounds(nodes),
                flowHeight: flowHeight - TOP_OFFSET,
                flowWidth,
            });

            void setViewport({...viewport, y: viewport.y + TOP_OFFSET});
        } else {
            void fitView({
                ...FIT_VIEW_OPTIONS,
                padding: {
                    bottom: FIT_VIEW_OPTIONS.padding,
                    left: FIT_VIEW_OPTIONS.padding,
                    right: FIT_VIEW_OPTIONS.padding,
                    top: `${TOP_OFFSET}px`,
                },
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [nodesInitialized, workflowUuid, customCanvasWidth, flowHeight, flowWidth, layoutDirection]);

    return null;
};

export default FitViewOnWorkflowChange;
