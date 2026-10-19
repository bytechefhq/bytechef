import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useNodesInitialized, useReactFlow} from '@xyflow/react';
import {useEffect} from 'react';

const FitViewOnWorkflowChange = ({customCanvasWidth}: {customCanvasWidth?: number}) => {
    const workflowUuid = useWorkflowDataStore((state) => state.workflow.workflowUuid);

    const {fitView} = useReactFlow();
    const nodesInitialized = useNodesInitialized();

    useEffect(() => {
        if (!nodesInitialized) {
            return;
        }

        fitView({maxZoom: 1, minZoom: 0.2, padding: {bottom: 0.2, left: 0.2, right: 0.2, top: '72px'}});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [nodesInitialized, workflowUuid, customCanvasWidth]);

    return null;
};

export default FitViewOnWorkflowChange;
