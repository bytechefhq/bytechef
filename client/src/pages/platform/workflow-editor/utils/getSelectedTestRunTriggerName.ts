import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import getTestRunTriggerName from '@/pages/platform/workflow-editor/utils/getTestRunTriggerName';
import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function getSelectedTestRunTriggerName(workflow: {
    triggers?: Array<Pick<WorkflowTrigger, 'name'>>;
}): string | undefined {
    if (!workflow.triggers?.length) {
        return undefined;
    }

    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore.getState();

    const selectedNodeName =
        workflowNodeDetailsPanelOpen && currentNode?.trigger ? currentNode.workflowNodeName : undefined;

    return getTestRunTriggerName(workflow, selectedNodeName);
}
