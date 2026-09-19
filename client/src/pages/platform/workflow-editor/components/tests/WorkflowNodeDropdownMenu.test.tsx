import WorkflowNodeDropdownMenu from '@/pages/platform/workflow-editor/components/WorkflowNodeDropdownMenu';
import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {NodeDataType} from '@/shared/types';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const nodeData = {
    componentName: 'slack',
    label: 'Slack',
    name: 'slack_1',
    workflowNodeName: 'slack_1',
} as NodeDataType;

const renderMenu = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <WorkflowNodeDropdownMenu
                data={nodeData}
                hasSavedPosition
                onCopy={vi.fn()}
                onCut={vi.fn()}
                onDelete={vi.fn()}
                onInfo={vi.fn()}
                onRename={vi.fn()}
                onResetPosition={vi.fn()}
                onSwitch={vi.fn()}
                showCopyAction
                showCutAction
                showDeleteAction
                showInfoAction
                showRenameAction
                trigger={
                    <button aria-label="slack_1 node actions" type="button">
                        Actions
                    </button>
                }
            />
        </WorkflowEditorReadOnlyContext.Provider>
    );

const getMenuItemLabels = () => screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent?.trim());

describe('WorkflowNodeDropdownMenu', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
    });

    it('offers every editing action when the editor is editable', async () => {
        renderMenu(false);

        await userEvent.click(screen.getByRole('button', {name: 'slack_1 node actions'}));

        expect(getMenuItemLabels()).toEqual(['Cut', 'Copy', 'Rename', 'Reset position', 'Info', 'Delete']);
    });

    it('offers only Info in read-only mode', async () => {
        renderMenu(true);

        await userEvent.click(screen.getByRole('button', {name: 'slack_1 node actions'}));

        expect(getMenuItemLabels()).toEqual(['Info']);
        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();
    });
});
