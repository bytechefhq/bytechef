import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import getChatTriggerName from '@/pages/platform/workflow-editor/utils/getChatTriggerName';
import getSelectedTestRunTriggerName from '@/pages/platform/workflow-editor/utils/getSelectedTestRunTriggerName';

export default function startChatTestSession() {
    const {workflow} = useWorkflowDataStore.getState();

    const chatTriggerName = getChatTriggerName(workflow, getSelectedTestRunTriggerName(workflow));

    useDataPillPanelStore.getState().setDataPillPanelOpen(false);
    useWorkflowNodeDetailsPanelStore.getState().setWorkflowNodeDetailsPanelOpen(false);
    useWorkflowTestChatStore.getState().startChatTestSession(chatTriggerName);
}
