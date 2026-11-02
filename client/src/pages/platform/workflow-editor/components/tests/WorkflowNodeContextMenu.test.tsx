import WorkflowNodeContextMenu from '@/pages/platform/workflow-editor/components/WorkflowNodeContextMenu';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {NodeDataType} from '@/shared/types';
import {fireEvent, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const triggerNodeData = {
    componentName: 'manual',
    label: 'Manual',
    name: 'trigger_2',
    trigger: true,
    workflowNodeName: 'trigger_2',
} as NodeDataType;

const renderContextMenu = (readOnly: boolean, onDelete = vi.fn()) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <WorkflowNodeContextMenu
                data={triggerNodeData}
                hasSavedPosition
                onDelete={onDelete}
                onInfo={vi.fn()}
                onRename={vi.fn()}
                onResetPosition={vi.fn()}
                onSwitch={vi.fn()}
                showDeleteAction
                showInfoAction
                showRenameAction
            >
                <div>trigger_2 node</div>
            </WorkflowNodeContextMenu>
        </WorkflowEditorReadOnlyContext.Provider>
    );

describe('WorkflowNodeContextMenu', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.restoreAllMocks();
    });

    it('lets an editor delete a trigger through the confirmation dialog', async () => {
        const onDelete = vi.fn();

        renderContextMenu(false, onDelete);

        fireEvent.contextMenu(screen.getByText('trigger_2 node'));

        fireEvent.click(await screen.findByRole('menuitem', {name: 'Delete'}));

        await userEvent.click(await screen.findByRole('button', {name: 'Delete node'}));

        expect(onDelete).toHaveBeenCalledTimes(1);
    });

    it('offers only Info on a trigger in read-only mode', async () => {
        renderContextMenu(true);

        fireEvent.contextMenu(screen.getByText('trigger_2 node'));

        await screen.findByRole('menuitem', {name: 'Info'});

        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();
    });

    it('clears the menu-ready timer on unmount', () => {
        const setTimeoutSpy = vi.spyOn(globalThis, 'setTimeout');
        const clearTimeoutSpy = vi.spyOn(globalThis, 'clearTimeout');

        const {unmount} = renderContextMenu(false);

        fireEvent.contextMenu(screen.getByText('trigger_2 node'));

        const menuReadyTimeoutIndex = setTimeoutSpy.mock.calls.findIndex((call) => call[1] === 200);

        expect(menuReadyTimeoutIndex).toBeGreaterThanOrEqual(0);

        const menuReadyTimeoutId = setTimeoutSpy.mock.results[menuReadyTimeoutIndex].value;

        unmount();

        expect(clearTimeoutSpy).toHaveBeenCalledWith(menuReadyTimeoutId);
    });
});
