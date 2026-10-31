import {TooltipProvider} from '@/components/ui/tooltip';
import {McpComponent, McpServer} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentListItem from './McpComponentListItem';

const hoisted = vi.hoisted(() => ({
    setShowEditDialog: vi.fn(),
    useMcpComponentListItem: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-component-list/hooks/useMcpComponentListItem', () => ({
    default: hoisted.useMcpComponentListItem,
}));

vi.mock('@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItemDropdownMenu', () => ({
    default: ({onEditClick}: {onEditClick: () => void}) => (
        <button onClick={onEditClick} type="button">
            Edit
        </button>
    ),
}));

vi.mock('../mcp-component-dialog/McpComponentDialog', () => ({
    default: ({open}: {open: boolean}) => (open ? <div data-testid="mcp-component-dialog" /> : null),
}));

vi.mock('./McpComponentToolList', () => ({
    default: () => <div data-testid="mcp-component-tool-list" />,
}));

const mcpServer = {id: '5', name: 'Server'} as McpServer;

const boundMcpComponent = {
    componentName: 'googleMail',
    componentVersion: 1,
    id: '7',
    integrationId: '6',
    lastModifiedDate: '2026-10-31T08:00:00Z',
    mcpTools: [],
    title: 'Gmail',
} as unknown as McpComponent;

const unboundMcpComponent = {...boundMcpComponent, integrationId: null} as McpComponent;

const renderItem = (mcpComponent: McpComponent) =>
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
            integrationName: 'Gmail Code',
            setShowEditDialog: hoisted.setShowEditDialog,
            showEditDialog: false,
        });
    });

    it('shows the integration a bound component belongs to', () => {
        renderItem(boundMcpComponent);

        expect(hoisted.useMcpComponentListItem).toHaveBeenCalledWith('googleMail', 1, '6');
        expect(screen.getByText('Gmail Code')).toBeInTheDocument();
        expect(screen.queryByText('No integration')).not.toBeInTheDocument();
        expect(screen.getByText('Gmail')).toBeInTheDocument();
        expect(screen.getByText('v1')).toBeInTheDocument();
    });

    it('shows no integration badge while the name of the bound integration loads', () => {
        hoisted.useMcpComponentListItem.mockReturnValue({
            componentDefinition: undefined,
            integrationName: undefined,
            setShowEditDialog: hoisted.setShowEditDialog,
            showEditDialog: false,
        });

        renderItem(boundMcpComponent);

        expect(screen.queryByText('Gmail Code')).not.toBeInTheDocument();
        expect(screen.queryByText('No integration')).not.toBeInTheDocument();
    });

    it('warns that a component without an integration needs one', () => {
        renderItem({...unboundMcpComponent, lastModifiedDate: null, title: null} as unknown as McpComponent);

        expect(screen.getByText('No integration')).toBeInTheDocument();
        expect(screen.queryByText('Gmail Code')).not.toBeInTheDocument();
        expect(screen.getByText('googleMail')).toBeInTheDocument();
        expect(screen.getByText('-')).toBeInTheDocument();
    });

    it('shows the tools of the component when it is expanded', async () => {
        renderItem(boundMcpComponent);

        expect(screen.queryByTestId('mcp-component-tool-list')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Show tools'}));

        expect(screen.getByTestId('mcp-component-tool-list')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Hide tools'})).toBeInTheDocument();
    });

    it('opens the edit dialog from the dropdown menu', async () => {
        renderItem(boundMcpComponent);

        await userEvent.click(screen.getByRole('button', {name: 'Edit'}));

        expect(hoisted.setShowEditDialog).toHaveBeenCalledWith(true);
    });

    it('renders the edit dialog while it is shown', () => {
        hoisted.useMcpComponentListItem.mockReturnValue({
            componentDefinition: {icon: '<svg></svg>'},
            integrationName: 'Gmail Code',
            setShowEditDialog: hoisted.setShowEditDialog,
            showEditDialog: true,
        });

        renderItem(boundMcpComponent);

        expect(screen.getByTestId('mcp-component-dialog')).toBeInTheDocument();
    });
});
