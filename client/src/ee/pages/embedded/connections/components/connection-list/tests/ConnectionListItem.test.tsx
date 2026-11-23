import {TooltipProvider} from '@/components/ui/tooltip';
import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionListItem from '../ConnectionListItem';

const hoisted = vi.hoisted(() => ({
    deleteConnectionMutate: vi.fn(),
    integrationConnectionWorkflows: [
        {
            id: '31',
            parentId: '5',
            parentName: 'HubSpot Sync',
            parentType: 'INTEGRATION',
            parentVersion: 3,
            usage: 'DEPLOYMENT',
            workflowId: 'workflow-3',
            workflowName: 'Push contacts',
            workflowUuid: 'uuid-3',
        },
        {
            id: '32',
            parentId: '1050',
            parentName: 'Jane Doe',
            parentType: 'CONNECTED_USER',
            parentVersion: 4,
            usage: 'EDITOR',
            workflowId: 'workflow-4',
            workflowName: 'Route invoice emails',
            workflowUuid: 'uuid-4',
        },
    ],
    openIntegrationWorkflow: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    IntegrationConnectionWorkflowParentType: {ConnectedUser: 'CONNECTED_USER', Integration: 'INTEGRATION'},
    useIntegrationConnectionWorkflowsQuery: (variables: unknown, options: {enabled: boolean}) => ({
        data: options.enabled ? {integrationConnectionWorkflows: hoisted.integrationConnectionWorkflows} : undefined,
        isLoading: false,
    }),
}));

vi.mock('@/ee/pages/embedded/shared/useOpenInEmbeddedEditor', () => ({
    default: () => ({openIntegrationWorkflow: hoisted.openIntegrationWorkflow}),
}));

vi.mock('@/components/LazyLoadSVG/LazyLoadSVG', () => ({
    default: () => <span data-testid="component-icon" />,
}));

vi.mock('@/ee/shared/mutations/embedded/connectionTags.mutations', () => ({
    useUpdateConnectionTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/embedded/connections.mutations', () => ({
    useDeleteConnectionMutation: () => ({mutate: hoisted.deleteConnectionMutate}),
    useUpdateConnectionCredentialsMutation: vi.fn(),
    useUpdateConnectionMutation: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetConnectionTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: ({
        onClose,
        useUpdateConnectionCredentialsMutation,
    }: {
        onClose: () => void;
        useUpdateConnectionCredentialsMutation?: unknown;
    }) => (
        <div
            data-has-credentials-mutation={String(useUpdateConnectionCredentialsMutation !== undefined)}
            data-testid="connection-dialog"
        >
            <button onClick={onClose} type="button">
                Close connection dialog
            </button>
        </div>
    ),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinitions: ['componentDefinitions']},
    useGetConnectionComponentDefinitionQuery: () => ({data: {icon: '<svg />', name: 'slack'}}),
}));

const connection: Connection = {
    componentName: 'slack',
    connectionVersion: 1,
    credentialStatus: 'VALID',
    id: 7,
    name: 'My Slack',
    parameters: {},
};

const renderConnectionListItem = () =>
    render(
        <TooltipProvider>
            <ul>
                <ConnectionListItem componentDefinitions={[]} connection={connection} />
            </ul>
        </TooltipProvider>
    );

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: ''}));
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ConnectionListItem', () => {
    it('should render with the design-system DropdownMenu', async () => {
        renderConnectionListItem();

        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('should open the edit dialog when clicking Edit', async () => {
        renderConnectionListItem();

        expect(screen.queryByTestId('connection-dialog')).not.toBeInTheDocument();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByTestId('connection-dialog')).toBeInTheDocument();
    });

    it('should open the delete dialog and delete the connection when clicking Delete', async () => {
        renderConnectionListItem();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        const deleteDialog = await screen.findByRole('alertdialog');

        expect(within(deleteDialog).getByText('Are you absolutely sure?')).toBeInTheDocument();

        await userEvent.click(within(deleteDialog).getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteConnectionMutate).toHaveBeenCalledWith(7);

        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    });

    it('passes the credentials mutation to the edit dialog', async () => {
        renderConnectionListItem();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByTestId('connection-dialog')).toHaveAttribute('data-has-credentials-mutation', 'true');
    });

    it('lists the integration and connected user workflows that use the connection, without a menu', async () => {
        renderConnectionListItem();

        await userEvent.click(screen.getByRole('button', {name: 'Workflows'}));

        const rows = within(screen.getByTestId('connection-workflows-list')).getAllByRole('listitem');

        expect(rows).toHaveLength(2);
        expect(rows[0]).toHaveTextContent('Push contacts');
        expect(rows[0]).toHaveTextContent('HubSpot Sync');
        expect(within(rows[0]).queryByRole('img', {name: 'Connected user'})).not.toBeInTheDocument();
        expect(rows[0]).toHaveTextContent('V3');
        expect(rows[0]).toHaveTextContent('Deployed');
        expect(rows[1]).toHaveTextContent('Route invoice emails');
        expect(rows[1]).toHaveTextContent('Jane Doe');
        expect(within(rows[1]).getByRole('img', {name: 'Connected user'})).toBeInTheDocument();
        expect(within(rows[1]).queryByRole('button', {name: 'Open in integration'})).not.toBeInTheDocument();

        await userEvent.click(within(rows[0]).getByRole('button', {name: 'Open in integration'}));

        expect(hoisted.openIntegrationWorkflow).toHaveBeenCalledWith(5, 'uuid-3');
        expect(rows[1]).not.toHaveTextContent('V4');
        expect(rows[1]).toHaveTextContent('Editor');
        expect(screen.queryByRole('button', {name: 'Workflow actions'})).not.toBeInTheDocument();
    });
});
