import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowIssuesStore from '@/pages/platform/workflow-editor/stores/useWorkflowIssuesStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';

import {OverlayPanelsStateI} from '../utils/overlayPanelViewport';

export default function useOverlayPanelsState(): OverlayPanelsStateI {
    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);
    const issuesSidebarOpen = useWorkflowIssuesStore((state) => state.issuesSidebarOpen);
    const rightSidebarOpen = useRightSidebarStore((state) => state.rightSidebarOpen);
    const workflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.workflowTestChatPanelOpen);
    const workflowNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore(
        (state) => state.workflowNodeDetailsPanelOpen
    );

    return {
        dataPillPanelOpen,
        issuesSidebarOpen,
        rightSidebarOpen,
        workflowNodeDetailsPanelOpen,
        workflowTestChatPanelOpen,
    };
}
