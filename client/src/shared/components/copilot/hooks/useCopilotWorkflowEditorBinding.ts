import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useEffect} from 'react';

const useCopilotWorkflowEditorBinding = (workflowKey: string | undefined) => {
    const bindWorkflowEditorWorkflow = useCopilotStore((state) => state.bindWorkflowEditorWorkflow);

    useEffect(() => {
        if (workflowKey) {
            bindWorkflowEditorWorkflow(workflowKey);
        }
    }, [bindWorkflowEditorWorkflow, workflowKey]);
};

export default useCopilotWorkflowEditorBinding;
