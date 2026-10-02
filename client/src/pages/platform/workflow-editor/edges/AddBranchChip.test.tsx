import {render, screen} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import AddBranchChip from './AddBranchChip';

const {popoverMenuSpy} = vi.hoisted(() => ({popoverMenuSpy: vi.fn()}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    EdgeLabelRenderer: ({children}: {children: ReactNode}) => <div data-testid="edge-label-renderer">{children}</div>,
}));

vi.mock('../components/WorkflowNodesPopoverMenu', () => ({
    default: ({children, ...props}: {children: ReactNode}) => {
        popoverMenuSpy(props);

        return <div data-testid="popover-menu">{children}</div>;
    },
}));

const PLACEHOLDER_ID = 'parallel_1-parallel-placeholder-0';

const canvasNodes = [
    {data: {}, id: 'trigger_1', position: {x: 0, y: 0}, type: 'workflow'},
    {data: {}, id: 'parallel_1', position: {x: 0, y: 0}, type: 'workflow'},
    {data: {label: '+'}, id: PLACEHOLDER_ID, position: {x: 0, y: 0}, type: 'placeholder'},
] as Array<Node>;

function renderChip() {
    return render(
        <AddBranchChip
            edgeId="parallel_1-lane-entry"
            layoutDirection="TB"
            placeholderId={PLACEHOLDER_ID}
            sourceX={100}
            sourceY={100}
            targetX={100}
            targetY={300}
        />
    );
}

describe('AddBranchChip', () => {
    beforeEach(() => {
        popoverMenuSpy.mockClear();

        useWorkflowDataStore.setState({nodes: canvasNodes, workflow: {id: 'workflow-1'}} as never);

        useWorkflowEditorStore.setState({copiedNode: undefined, copiedWorkflowId: undefined} as never);
    });

    it('should render nothing when the placeholder is no longer on the canvas', () => {
        useWorkflowDataStore.setState({
            nodes: canvasNodes.filter((node) => node.id !== PLACEHOLDER_ID),
        } as never);

        renderChip();

        expect(screen.queryByTestId('popover-menu')).toBeNull();
    });

    it('should hand the menu the placeholder position on the canvas', () => {
        renderChip();

        expect(screen.getByRole('button', {name: 'Add branch'})).toBeTruthy();

        expect(popoverMenuSpy).toHaveBeenCalledWith(
            expect.objectContaining({nodeIndex: 2, sourceNodeId: PLACEHOLDER_ID})
        );
    });

    it('should allow pasting a node copied from the same workflow', () => {
        useWorkflowEditorStore.setState({
            copiedNode: {componentName: 'logger', trigger: false},
            copiedWorkflowId: 'workflow-1',
        } as never);

        renderChip();

        expect(popoverMenuSpy).toHaveBeenCalledWith(expect.objectContaining({showPaste: true}));
    });

    it('should not allow pasting a copied trigger or a node copied from another workflow', () => {
        useWorkflowEditorStore.setState({
            copiedNode: {componentName: 'schedule', trigger: true},
            copiedWorkflowId: 'workflow-1',
        } as never);

        renderChip();

        expect(popoverMenuSpy).toHaveBeenLastCalledWith(expect.objectContaining({showPaste: false}));

        useWorkflowEditorStore.setState({
            copiedNode: {componentName: 'logger', trigger: false},
            copiedWorkflowId: 'workflow-2',
        } as never);

        renderChip();

        expect(popoverMenuSpy).toHaveBeenLastCalledWith(expect.objectContaining({showPaste: false}));
    });
});
