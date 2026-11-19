import {TooltipProvider} from '@/components/ui/tooltip';
import {McpComponent, McpServer} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentListItem from '../McpComponentListItem';

const hoisted = vi.hoisted(() => ({
    setShowEditDialog: vi.fn(),
    useMcpComponentListItem: vi.fn(),
}));

vi.mock('@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentListItem', () => ({
    default: hoisted.useMcpComponentListItem,
}));

vi.mock('@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItemDropdownMenu', () => ({
    default: ({onEditClick}: {onEditClick: () => void}) => (
        <button onClick={onEditClick} type="button">
            Edit
        </button>
    ),
}));

vi.mock('../../mcp-component-dialog/McpComponentDialog', () => ({
    default: ({open}: {open: boolean}) => (open ? <div data-testid="mcp-component-dialog" /> : null),
}));

vi.mock('../McpComponentToolList', () => ({
    default: ({
        embedded,
        renderEditDialog,
    }: {
        embedded?: boolean;
        renderEditDialog: (open: boolean, onOpenChange: (open: boolean) => void) => ReactNode;
    }) => (
        <div data-embedded={String(embedded)} data-testid="mcp-component-tool-list">
            {renderEditDialog(true, () => undefined)}
        </div>
    ),
}));

const mcpServer = {id: '5', name: 'Server'} as McpServer;

const mcpComponent = {
    componentName: 'affinity',
    componentVersion: 2,
    id: '7',
    lastModifiedDate: '2026-10-31T08:00:00Z',
    mcpTools: [],
    title: 'Affinity',
} as unknown as McpComponent;

const renderItem = () =>
    render(
        <TooltipProvider>
            <McpComponentListItem mcpComponent={mcpComponent} mcpServer={mcpServer} />
        </TooltipProvider>
    );

describe('McpComponentListItem', () => {
    beforeEach(() => {
        hoisted.setShowEditDialog.mockReset();
        hoisted.useMcpComponentListItem.mockReset();
        hoisted.useMcpComponentListItem.mockReturnValue({
            componentDefinition: undefined,
            setShowEditDialog: hoisted.setShowEditDialog,
            showEditDialog: false,
        });
    });

    it('shows the title and version of the component without integration badges', () => {
        renderItem();

        expect(hoisted.useMcpComponentListItem).toHaveBeenCalledWith('affinity', 2);
        expect(screen.getByText('Affinity')).toBeInTheDocument();
        expect(screen.getByText('v2')).toBeInTheDocument();
        expect(screen.queryByText('No integration')).not.toBeInTheDocument();
    });

    it('shows the automation tools of the component, with their edit dialog, when it is expanded', async () => {
        renderItem();

        expect(screen.queryByTestId('mcp-component-tool-list')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Show tools'}));

        expect(screen.getByTestId('mcp-component-tool-list')).not.toHaveAttribute('data-embedded', 'true');
        expect(screen.getByTestId('mcp-component-dialog')).toBeInTheDocument();
    });

    it('opens the edit dialog from the dropdown menu', async () => {
        renderItem();

        await userEvent.click(screen.getByRole('button', {name: 'Edit'}));

        expect(hoisted.setShowEditDialog).toHaveBeenCalledWith(true);
    });
});
