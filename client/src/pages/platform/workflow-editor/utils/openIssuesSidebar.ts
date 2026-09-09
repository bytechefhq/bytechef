import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowIssuesStore from '@/pages/platform/workflow-editor/stores/useWorkflowIssuesStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';

export default function openIssuesSidebar(): void {
    useWorkflowNodeDetailsPanelStore.getState().setWorkflowNodeDetailsPanelOpen(false);
    useWorkflowTestChatStore.getState().setWorkflowTestChatPanelOpen(false);
    useRightSidebarStore.getState().setRightSidebarOpen(false);
    useWorkflowIssuesStore.getState().setIssuesSidebarOpen(true);
}
