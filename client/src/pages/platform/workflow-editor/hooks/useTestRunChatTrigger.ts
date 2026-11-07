import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import getSelectedTriggerNodeName from '@/pages/platform/workflow-editor/utils/getSelectedTriggerNodeName';
import getTestRunTriggerName from '@/pages/platform/workflow-editor/utils/getTestRunTriggerName';
import isChatTriggerType from '@/pages/platform/workflow-editor/utils/isChatTriggerType';
import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

export default function useTestRunChatTrigger(workflow: {
    triggers?: Array<Pick<WorkflowTrigger, 'name' | 'type'>>;
}): boolean {
    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentNode: state.currentNode,
            workflowNodeDetailsPanelOpen: state.workflowNodeDetailsPanelOpen,
        }))
    );
    const workflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.workflowTestChatPanelOpen);

    return useMemo(() => {
        if (workflowTestChatPanelOpen) {
            return true;
        }

        const testRunTriggerName = getTestRunTriggerName(
            workflow,
            getSelectedTriggerNodeName({currentNode, workflowNodeDetailsPanelOpen})
        );

        const testRunTrigger = workflow.triggers?.find((trigger) => trigger.name === testRunTriggerName);

        return isChatTriggerType(testRunTrigger?.type);
    }, [currentNode, workflow, workflowNodeDetailsPanelOpen, workflowTestChatPanelOpen]);
}
