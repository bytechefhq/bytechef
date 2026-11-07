import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import getSelectedTriggerNodeName from '@/pages/platform/workflow-editor/utils/getSelectedTriggerNodeName';
import getTestRunTriggerName from '@/pages/platform/workflow-editor/utils/getTestRunTriggerName';
import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';

export default function getSelectedTestRunTriggerName(workflow: {
    triggers?: Array<Pick<WorkflowTrigger, 'name'>>;
}): string | undefined {
    if (!workflow.triggers?.length) {
        return undefined;
    }

    return getTestRunTriggerName(workflow, getSelectedTriggerNodeName(useWorkflowNodeDetailsPanelStore.getState()));
}
