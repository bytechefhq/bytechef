import WorkflowNodeDropdownMenu from '@/pages/platform/workflow-editor/components/WorkflowNodeDropdownMenu';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {NodeDataType} from '@/shared/types';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    onCopy: vi.fn(),
    onCut: vi.fn(),
    onDelete: vi.fn(),
    onInfo: vi.fn(),
    onOpenChange: vi.fn(),
    onPaste: vi.fn(),
    onRename: vi.fn(),
    onResetPosition: vi.fn(),
    onSwitch: vi.fn(),
}));

const actionNode = {
    componentName: 'httpClient',
    label: 'HTTP Client',
    name: 'httpClient_1',
    workflowNodeName: 'httpClient_1',
} as NodeDataType;

const triggerNode = {...actionNode, trigger: true} as NodeDataType;

const copiedNode = {
    componentName: 'slack',
    label: 'Slack',
    name: 'slack_1',
    workflowNodeName: 'slack_1',
} as NodeDataType;

const renderWorkflowNodeDropdownMenu = (
    props: Partial<Parameters<typeof WorkflowNodeDropdownMenu>[0]> = {},
    data: NodeDataType = actionNode
) =>
    render(
        <WorkflowNodeDropdownMenu
            canPaste={false}
            data={data}
            hasSavedPosition={true}
            onCopy={hoisted.onCopy}
            onCut={hoisted.onCut}
            onDelete={hoisted.onDelete}
            onInfo={hoisted.onInfo}
            onOpenChange={hoisted.onOpenChange}
            onPaste={hoisted.onPaste}
            onRename={hoisted.onRename}
            onResetPosition={hoisted.onResetPosition}
            onSwitch={hoisted.onSwitch}
            showCopyAction={true}
            showCutAction={true}
            showDeleteAction={true}
            showInfoAction={true}
            showRenameAction={true}
            showReplaceAction={true}
            trigger={<button type="button">Node menu</button>}
            {...props}
        />
    );

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Node menu'}));
};

beforeEach(() => {
    windowResizeObserver();

    useWorkflowEditorStore.setState({copiedNode: undefined});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('WorkflowNodeDropdownMenu', () => {
    it('should render with the design-system DropdownMenu', async () => {
        renderWorkflowNodeDropdownMenu();

        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Cut'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('should offer every action of an action node in order', async () => {
        renderWorkflowNodeDropdownMenu();

        await openMenu();

        expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent)).toEqual([
            'Cut',
            'Replace',
            'Copy',
            'Rename',
            'Reset position',
            'Info',
            'Delete',
        ]);
        expect(screen.getAllByRole('separator')).toHaveLength(2);
    });

    it('should offer only the trigger actions for a trigger node', async () => {
        renderWorkflowNodeDropdownMenu({}, triggerNode);

        await openMenu();

        expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent)).toEqual([
            'Replace',
            'Rename',
            'Info',
            'Delete',
        ]);
    });

    it('should call onOpenChange when the menu opens', async () => {
        renderWorkflowNodeDropdownMenu();

        await openMenu();

        expect(hoisted.onOpenChange).toHaveBeenCalledWith(true);
    });

    it.each([
        ['Cut', 'onCut'],
        ['Replace', 'onSwitch'],
        ['Copy', 'onCopy'],
        ['Rename', 'onRename'],
        ['Reset position', 'onResetPosition'],
        ['Info', 'onInfo'],
    ] as const)('should call the handler of the %s row', async (rowName, handlerName) => {
        renderWorkflowNodeDropdownMenu();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: rowName}));

        expect(hoisted[handlerName]).toHaveBeenCalledTimes(1);
    });

    it('should paste after the node, showing the copied node, when clicking Paste After', async () => {
        useWorkflowEditorStore.setState({copiedNode});

        renderWorkflowNodeDropdownMenu({canPaste: true});

        await openMenu();

        const pasteMenuItem = screen.getByRole('menuitem', {name: /Paste After/});

        expect(pasteMenuItem).toHaveTextContent('Slack (slack_1)');

        await userEvent.click(pasteMenuItem);

        expect(hoisted.onPaste).toHaveBeenCalledTimes(1);
    });

    it('should not offer Paste After when nothing is copied', async () => {
        renderWorkflowNodeDropdownMenu({canPaste: true});

        await openMenu();

        expect(screen.queryByRole('menuitem', {name: /Paste After/})).not.toBeInTheDocument();
    });

    it('should confirm in a dialog before deleting the node', async () => {
        renderWorkflowNodeDropdownMenu();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(hoisted.onDelete).not.toHaveBeenCalled();
        expect(await screen.findByRole('alertdialog')).toHaveTextContent('Delete node HTTP Client?');

        await userEvent.click(screen.getByRole('button', {name: 'Delete node'}));

        expect(hoisted.onDelete).toHaveBeenCalledTimes(1);
    });

    it('should close the dialog without deleting the node when keeping it', async () => {
        renderWorkflowNodeDropdownMenu();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        await userEvent.click(await screen.findByRole('button', {name: 'Keep node'}));

        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());

        expect(hoisted.onDelete).not.toHaveBeenCalled();
    });

    it('should not offer Delete or render the dialog when deleting is not allowed', async () => {
        renderWorkflowNodeDropdownMenu({showDeleteAction: false});

        await openMenu();

        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });
});
