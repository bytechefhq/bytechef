import {DATA_PILL_PANEL_WIDTH, NODE_DETAILS_PANEL_WIDTH, WORKFLOW_NODES_SIDEBAR_WIDTH} from '@/shared/constants';
import {describe, expect, it} from 'vitest';

import {OverlayPanelsStateI, computeOverlayViewportOffset} from './overlayPanelViewport';

const allClosed: OverlayPanelsStateI = {
    dataPillPanelOpen: false,
    issuesSidebarOpen: false,
    rightSidebarOpen: false,
    workflowNodeDetailsPanelOpen: false,
    workflowTestChatPanelOpen: false,
};

describe('computeOverlayViewportOffset', () => {
    it('is zero when no overlay is open', () => {
        expect(computeOverlayViewportOffset(allClosed)).toBe(0);
    });

    it('re-centres by half the width of each open overlay', () => {
        expect(computeOverlayViewportOffset({...allClosed, rightSidebarOpen: true})).toBe(
            -WORKFLOW_NODES_SIDEBAR_WIDTH / 2
        );
        expect(computeOverlayViewportOffset({...allClosed, issuesSidebarOpen: true})).toBe(
            -WORKFLOW_NODES_SIDEBAR_WIDTH / 2
        );
        expect(computeOverlayViewportOffset({...allClosed, workflowTestChatPanelOpen: true})).toBe(
            -NODE_DETAILS_PANEL_WIDTH / 2
        );
        expect(computeOverlayViewportOffset({...allClosed, workflowNodeDetailsPanelOpen: true})).toBe(
            -NODE_DETAILS_PANEL_WIDTH / 2
        );
    });

    it('adds the data pill panel on top of the node details panel', () => {
        expect(
            computeOverlayViewportOffset({...allClosed, dataPillPanelOpen: true, workflowNodeDetailsPanelOpen: true})
        ).toBe(-(NODE_DETAILS_PANEL_WIDTH + DATA_PILL_PANEL_WIDTH) / 2);
    });
});
