import {DATA_PILL_PANEL_WIDTH, NODE_DETAILS_PANEL_WIDTH, WORKFLOW_NODES_SIDEBAR_WIDTH} from '@/shared/constants';

export interface OverlayPanelsStateI {
    dataPillPanelOpen: boolean;
    issuesSidebarOpen: boolean;
    rightSidebarOpen: boolean;
    workflowNodeDetailsPanelOpen: boolean;
    workflowTestChatPanelOpen: boolean;
}

export function computeOverlayViewportOffset({
    dataPillPanelOpen,
    issuesSidebarOpen,
    rightSidebarOpen,
    workflowNodeDetailsPanelOpen,
    workflowTestChatPanelOpen,
}: OverlayPanelsStateI): number {
    let offset = 0;

    if (rightSidebarOpen) {
        offset -= WORKFLOW_NODES_SIDEBAR_WIDTH / 2;
    }

    if (issuesSidebarOpen) {
        offset -= WORKFLOW_NODES_SIDEBAR_WIDTH / 2;
    }

    if (workflowTestChatPanelOpen) {
        offset -= NODE_DETAILS_PANEL_WIDTH / 2;
    }

    if (workflowNodeDetailsPanelOpen) {
        offset -= NODE_DETAILS_PANEL_WIDTH / 2;
    }

    if (dataPillPanelOpen) {
        offset -= DATA_PILL_PANEL_WIDTH / 2;
    }

    return offset;
}
