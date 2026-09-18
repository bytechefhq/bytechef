import {act, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {MouseEvent} from 'react';
import {createPortal} from 'react-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';

vi.mock('../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: undefined}),
}));

vi.mock('../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: object) => unknown) => selector({edges: [], nodes: [], workflow: {id: 'workflow-1'}}),
}));

vi.mock('../components/WorkflowNodesPopoverMenuComponentList', () => ({
    default: ({sourceNodeId}: {sourceNodeId: string}) => (
        <div>
            {`Components for ${sourceNodeId}`}

            {createPortal(<button type="button">{`Portaled filter for ${sourceNodeId}`}</button>, document.body)}
        </div>
    ),
}));

vi.mock('../components/WorkflowNodesPopoverMenuOperationList', () => ({
    default: () => <div>Operations</div>,
}));

const stopPropagation = (event: MouseEvent) => event.stopPropagation();

const waitForListeners = () =>
    act(async () => {
        await new Promise((resolve) => setTimeout(resolve, 0));
    });

// An open modal popover blocks pointer events and hides the rest of the page from the accessibility tree, the same
// way it does in the app, where canvas nodes stay clickable through React Flow's own pointer-events rule.
const clickText = async (text: string) => {
    await userEvent.setup({pointerEventsCheck: 0}).click(screen.getByText(text));

    await waitForListeners();
};

describe('WorkflowNodesPopoverMenu', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
    });

    it('should close when something outside it is pressed, even if that stops the event from propagating', async () => {
        render(
            <>
                <WorkflowNodesPopoverMenu sourceNodeId="nodeA">
                    <button type="button">Add after A</button>
                </WorkflowNodesPopoverMenu>

                {/* Stands in for a locked React Flow node: d3-zoom stops mousedown, node buttons stop click. */}
                <button onClick={stopPropagation} onMouseDown={stopPropagation} type="button">
                    Other node
                </button>
            </>
        );

        await clickText('Add after A');

        expect(screen.getByText('Components for nodeA')).toBeInTheDocument();

        await clickText('Other node');

        expect(screen.queryByText('Components for nodeA')).not.toBeInTheDocument();
    });

    it('should close the open popover menu when another one is opened', async () => {
        render(
            <>
                <WorkflowNodesPopoverMenu sourceNodeId="nodeA">
                    <button type="button">Add after A</button>
                </WorkflowNodesPopoverMenu>

                <WorkflowNodesPopoverMenu sourceNodeId="nodeB">
                    <button type="button">Add after B</button>
                </WorkflowNodesPopoverMenu>
            </>
        );

        await clickText('Add after A');
        await clickText('Add after B');

        expect(screen.getByText('Components for nodeB')).toBeInTheDocument();
        expect(screen.queryByText('Components for nodeA')).not.toBeInTheDocument();
    });

    it('should tell the owner of an externally controlled popover menu to close it when something outside is pressed', async () => {
        const handleOpenChange = vi.fn();

        render(
            <>
                <WorkflowNodesPopoverMenu onOpenChange={handleOpenChange} open sourceNodeId="nodeA" />

                <button onClick={stopPropagation} onMouseDown={stopPropagation} type="button">
                    Other node
                </button>
            </>
        );

        await waitForListeners();

        await clickText('Other node');

        expect(handleOpenChange).toHaveBeenCalledWith(false);
    });

    it('should stay open when content it renders in a portal is pressed', async () => {
        render(
            <WorkflowNodesPopoverMenu sourceNodeId="nodeA">
                <button type="button">Add after A</button>
            </WorkflowNodesPopoverMenu>
        );

        await clickText('Add after A');
        await clickText('Portaled filter for nodeA');

        expect(screen.getByText('Components for nodeA')).toBeInTheDocument();
    });

    it('should close when its own trigger is clicked again', async () => {
        render(
            <WorkflowNodesPopoverMenu sourceNodeId="nodeA">
                <button type="button">Add after A</button>
            </WorkflowNodesPopoverMenu>
        );

        await clickText('Add after A');
        await clickText('Add after A');

        expect(screen.queryByText('Components for nodeA')).not.toBeInTheDocument();
    });
});
