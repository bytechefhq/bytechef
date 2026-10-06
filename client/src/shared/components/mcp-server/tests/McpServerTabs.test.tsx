import {McpServer} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerTabs, {
    McpServerComponentDialogProps,
    McpServerToolsContentProps,
    McpServerWorkflowDialogProps,
} from '../McpServerTabs';

const FakeMcpComponentDialog = ({mcpServerId, onOpenChange}: McpServerComponentDialogProps) => (
    <div role="dialog">
        <span>Component dialog for {mcpServerId}</span>

        <button onClick={() => onOpenChange(false)}>Close component dialog</button>
    </div>
);

const FakeWorkflowDialog = ({mcpServer, onClose}: McpServerWorkflowDialogProps) => (
    <div role="dialog">
        <span>Workflow dialog for {mcpServer.name}</span>

        <button onClick={onClose}>Close workflow dialog</button>
    </div>
);

const FakeToolsContent = ({activeToolsTab, onAddComponentClick, onAddWorkflowsClick}: McpServerToolsContentProps) => (
    <div>
        <span>{`Showing ${activeToolsTab}`}</span>

        <button onClick={onAddComponentClick}>Empty state Add Component</button>

        <button onClick={onAddWorkflowsClick}>Empty state Add Workflows</button>
    </div>
);

const renderTabs = () =>
    render(
        <McpServerTabs
            connectContent={<div>Connect content</div>}
            mcpComponentDialog={FakeMcpComponentDialog}
            mcpServer={{id: '1', name: 'mcpserver1'} as McpServer}
            toolsContent={FakeToolsContent}
            workflowDialog={FakeWorkflowDialog}
        />
    );

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpServerTabs', () => {
    it('opens on the Component Tools tab with an Add Component button', () => {
        renderTabs();

        expect(screen.getByText('Showing components')).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Component Tools', selected: true})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add Component'})).toBeInTheDocument();
    });

    it('opens the component dialog from the Add Component button and closes it again', async () => {
        renderTabs();

        await userEvent.click(screen.getByRole('button', {name: 'Add Component'}));

        expect(screen.getByText('Component dialog for 1')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Close component dialog'}));

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('switches the button to Add Workflows on the Workflow Tools tab', async () => {
        renderTabs();

        await userEvent.click(screen.getByRole('tab', {name: 'Workflow Tools'}));

        expect(screen.getByText('Showing workflows')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add Component'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Add Workflows'}));

        expect(screen.getByText('Workflow dialog for mcpserver1')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Close workflow dialog'}));

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('lets the tools content open both dialogs', async () => {
        renderTabs();

        await userEvent.click(screen.getByRole('button', {name: 'Empty state Add Component'}));

        expect(screen.getByText('Component dialog for 1')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Close component dialog'}));
        await userEvent.click(screen.getByRole('button', {name: 'Empty state Add Workflows'}));

        expect(screen.getByText('Workflow dialog for mcpserver1')).toBeInTheDocument();
    });

    it('shows Component Tools, Workflow Tools and Connect as the server tabs', () => {
        renderTabs();

        expect(screen.getAllByRole('tab').map((tab) => tab.textContent)).toEqual([
            'Component Tools',
            'Workflow Tools',
            'Connect',
        ]);
    });

    it('hides the Add button on the Connect tab', async () => {
        renderTabs();

        await userEvent.click(screen.getByRole('tab', {name: 'Connect'}));

        expect(screen.getByText('Connect content')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add Component'})).not.toBeInTheDocument();
    });
});
