import {fireEvent, render, screen, waitFor} from '@/shared/util/test-utils';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionTabConnectionSelect from './ConnectionTabConnectionSelect';

// Mock the workflow editor provider
const mockUseWorkflowEditor = vi.fn();
const mockUseWorkflowNodeDetailsPanelStore = vi.fn();
const mockUseWorkflowEditorStore = vi.fn();
const mockUseEnvironmentStore = vi.fn();

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => mockUseWorkflowEditor(),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: unknown) => mockUseWorkflowNodeDetailsPanelStore(selector),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: (selector: unknown) => mockUseWorkflowEditorStore(selector),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: unknown) => mockUseEnvironmentStore(selector),
}));

// Mock mutations
const mockSaveWorkflowTestConfigurationConnectionMutation = vi.fn();
const mockDeleteWorkflowTestConfigurationConnectionMutation = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useSaveWorkflowTestConfigurationConnectionMutation: ({onSuccess}: {onSuccess?: () => void}) => ({
        mutate: mockSaveWorkflowTestConfigurationConnectionMutation,
        onSuccess,
    }),
}));

vi.mock('@/shared/mutations/platform/workflowTestConfigurations.mutations', () => ({
    useDeleteWorkflowTestConfigurationConnectionMutation: ({onSuccess}: {onSuccess?: () => void}) => ({
        mutate: mockDeleteWorkflowTestConfigurationConnectionMutation,
        onSuccess,
    }),
}));

// Mock queries
const mockUseGetConnectionDefinitionQuery = vi.fn();
vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: () => mockUseGetConnectionDefinitionQuery(),
}));

// Mock ConnectionDialog component
vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: ({
        onClose,
        onConnectionCreate,
    }: {
        onClose: () => void;
        onConnectionCreate: (connectionId: number) => Promise<void>;
    }) => (
        <div data-testid="connection-dialog">
            <button data-testid="close-dialog" onClick={onClose}>
                Close
            </button>

            <button data-testid="create-connection" onClick={() => onConnectionCreate(123)}>
                Create Connection
            </button>
        </div>
    ),
}));

// Mock other components
vi.mock('@/shared/components/connection/ConnectionParameters', () => ({
    default: () => <div data-testid="connection-parameters">Connection Parameters</div>,
}));

vi.mock('@/shared/components/EnvironmentBadge', () => ({
    default: () => <div data-testid="environment-badge">Environment Badge</div>,
}));

// Mock query client
let mockQueryClient: QueryClient;
let mockInvalidateQueries: ReturnType<typeof vi.fn>;
let mockRemoveQueries: ReturnType<typeof vi.fn>;

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: () => mockQueryClient,
    };
});

describe('ConnectionTabConnectionSelect', () => {
    let mockSetCurrentNode: ReturnType<typeof vi.fn>;
    let mockSetCurrentComponent: ReturnType<typeof vi.fn>;

    const mockComponentConnection = {
        componentName: 'test-component',
        componentVersion: 1,
        key: 'connection_1',
        required: true,
        workflowNodeName: 'node-1',
    };

    const mockComponentDefinition = {
        clusterElement: false,
        clusterRoot: false,
        connectionRequired: true,
        name: 'test-component',
        title: 'Test Component',
        version: 1,
    };

    const mockConnections = [
        {
            environmentId: 1,
            id: 1,
            name: 'Test Connection 1',
            tags: [{name: 'tag1'}],
        },
        {
            environmentId: 1,
            id: 2,
            name: 'Test Connection 2',
            tags: [{name: 'tag2'}],
        },
    ];

    beforeEach(() => {
        vi.clearAllMocks();

        mockInvalidateQueries = vi.fn().mockResolvedValue(undefined);
        mockRemoveQueries = vi.fn();
        mockSetCurrentNode = vi.fn();
        mockSetCurrentComponent = vi.fn();

        mockQueryClient = {
            invalidateQueries: mockInvalidateQueries,
            removeQueries: mockRemoveQueries,
        } as unknown as QueryClient;

        // Mock environment store
        mockUseEnvironmentStore.mockReturnValue(1);

        // Mock workflow node details panel store
        mockUseWorkflowNodeDetailsPanelStore.mockReturnValue({
            connectionDialogAllowed: true,
            currentComponent: {id: 'comp1'},
            currentNode: {id: 'node1'},
            setCurrentComponent: mockSetCurrentComponent,
            setCurrentNode: mockSetCurrentNode,
        });

        // Mock workflow editor store
        mockUseWorkflowEditorStore.mockReturnValue({
            rootClusterElementNodeData: null,
        });

        // Mock workflow editor provider
        mockUseWorkflowEditor.mockReturnValue({
            ConnectionKeys: {
                connectionTags: ['connectionTags'],
                connections: ['connections'],
            },
            useCreateConnectionMutation: vi.fn(),
            useGetComponentDefinitionsQuery: () => ({data: [mockComponentDefinition]}),
            useGetConnectionTagsQuery: vi.fn(),
            useGetConnectionsQuery: () => ({data: mockConnections}),
        });

        // Mock connection definition query
        mockUseGetConnectionDefinitionQuery.mockReturnValue({
            data: {
                authorizationTypes: ['oauth2'],
                componentName: 'test-component',
            },
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('should render the component with connection select', () => {
        render(
            <ConnectionTabConnectionSelect
                componentConnection={mockComponentConnection}
                componentConnectionsCount={1}
                componentDefinition={mockComponentDefinition}
                workflowId="workflow-1"
                workflowNodeName="node-1"
            />
        );

        expect(screen.getByText('Test Component')).toBeInTheDocument();
        expect(screen.getByText('Choose Connection...')).toBeInTheDocument();
    });

    it('should display create connection button when connections exist', () => {
        render(
            <ConnectionTabConnectionSelect
                componentConnection={mockComponentConnection}
                componentConnectionsCount={1}
                componentDefinition={mockComponentDefinition}
                workflowId="workflow-1"
                workflowNodeName="node-1"
            />
        );

        const createButton = screen.getByTitle('Create a new connection');

        expect(createButton).toBeInTheDocument();
    });

    it('should open connection dialog when create button is clicked', () => {
        render(
            <ConnectionTabConnectionSelect
                componentConnection={mockComponentConnection}
                componentConnectionsCount={1}
                componentDefinition={mockComponentDefinition}
                workflowId="workflow-1"
                workflowNodeName="node-1"
            />
        );

        const createButton = screen.getByTitle('Create a new connection');

        fireEvent.click(createButton);

        expect(screen.getByTestId('connection-dialog')).toBeInTheDocument();
    });

    it('should close connection dialog when close button is clicked', async () => {
        render(
            <ConnectionTabConnectionSelect
                componentConnection={mockComponentConnection}
                componentConnectionsCount={1}
                componentDefinition={mockComponentDefinition}
                workflowId="workflow-1"
                workflowNodeName="node-1"
            />
        );

        const createButton = screen.getByTitle('Create a new connection');

        fireEvent.click(createButton);

        expect(screen.getByTestId('connection-dialog')).toBeInTheDocument();

        const closeButton = screen.getByTestId('close-dialog');

        fireEvent.click(closeButton);

        await waitFor(() => {
            expect(screen.queryByTestId('connection-dialog')).not.toBeInTheDocument();
        });
    });

    describe('onConnectionCreate callback', () => {
        it('should invalidate queries before setting connection ID', async () => {
            const callOrder: string[] = [];

            mockInvalidateQueries.mockImplementation(() => {
                callOrder.push('invalidateQueries');

                return Promise.resolve();
            });

            mockSaveWorkflowTestConfigurationConnectionMutation.mockImplementation(() => {
                callOrder.push('saveConnection');
            });

            render(
                <ConnectionTabConnectionSelect
                    componentConnection={mockComponentConnection}
                    componentConnectionsCount={1}
                    componentDefinition={mockComponentDefinition}
                    workflowId="workflow-1"
                    workflowNodeName="node-1"
                />
            );

            // Open dialog
            const createButton = screen.getByTitle('Create a new connection');

            fireEvent.click(createButton);

            // Trigger connection creation
            const createConnectionButton = screen.getByTestId('create-connection');

            fireEvent.click(createConnectionButton);

            await waitFor(() => {
                expect(callOrder).toEqual(['invalidateQueries', 'saveConnection']);
                expect(mockInvalidateQueries).toHaveBeenCalledWith({
                    queryKey: ['connections'],
                });
                expect(mockSaveWorkflowTestConfigurationConnectionMutation).toHaveBeenCalledWith({
                    connectionId: 123,
                    environmentId: 1,
                    workflowConnectionKey: 'connection_1',
                    workflowId: 'workflow-1',
                    workflowNodeName: 'node-1',
                });
            });
        });

        it('should update current node and component after connection creation', async () => {
            render(
                <ConnectionTabConnectionSelect
                    componentConnection={mockComponentConnection}
                    componentConnectionsCount={1}
                    componentDefinition={mockComponentDefinition}
                    workflowId="workflow-1"
                    workflowNodeName="node-1"
                />
            );

            // Open dialog
            const createButton = screen.getByTitle('Create a new connection');

            fireEvent.click(createButton);

            // Trigger connection creation
            const createConnectionButton = screen.getByTestId('create-connection');

            fireEvent.click(createConnectionButton);

            await waitFor(() => {
                expect(mockSetCurrentNode).toHaveBeenCalledWith({
                    connectionId: 123,
                    id: 'node1',
                });
                expect(mockSetCurrentComponent).toHaveBeenCalledWith({
                    connectionId: 123,
                    id: 'comp1',
                });
            });
        });

        it('should remove workflow node queries after connection creation', async () => {
            render(
                <ConnectionTabConnectionSelect
                    componentConnection={mockComponentConnection}
                    componentConnectionsCount={1}
                    componentDefinition={mockComponentDefinition}
                    workflowId="workflow-1"
                    workflowNodeName="node-1"
                />
            );

            // Open dialog
            const createButton = screen.getByTitle('Create a new connection');

            fireEvent.click(createButton);

            // Trigger connection creation
            const createConnectionButton = screen.getByTestId('create-connection');

            fireEvent.click(createConnectionButton);

            await waitFor(() => {
                expect(mockRemoveQueries).toHaveBeenCalledTimes(3);
            });
        });

        it('should handle connection creation with correct parameters', async () => {
            render(
                <ConnectionTabConnectionSelect
                    componentConnection={mockComponentConnection}
                    componentConnectionsCount={1}
                    componentDefinition={mockComponentDefinition}
                    workflowId="workflow-1"
                    workflowNodeName="node-1"
                />
            );

            // Open dialog
            const createButton = screen.getByTitle('Create a new connection');

            fireEvent.click(createButton);

            // Trigger connection creation
            const createConnectionButton = screen.getByTestId('create-connection');

            fireEvent.click(createConnectionButton);

            await waitFor(() => {
                expect(mockSaveWorkflowTestConfigurationConnectionMutation).toHaveBeenCalledWith({
                    connectionId: 123,
                    environmentId: 1,
                    workflowConnectionKey: 'connection_1',
                    workflowId: 'workflow-1',
                    workflowNodeName: 'node-1',
                });
            });
        });
    });

    it('should display selected connection value', () => {
        const workflowTestConfigurationConnection = {
            connectionId: 1,
            workflowConnectionKey: 'connection_1',
            workflowNodeName: 'node-1',
        };

        render(
            <ConnectionTabConnectionSelect
                componentConnection={mockComponentConnection}
                componentConnectionsCount={1}
                componentDefinition={mockComponentDefinition}
                workflowId="workflow-1"
                workflowNodeName="node-1"
                workflowTestConfigurationConnection={workflowTestConfigurationConnection}
            />
        );

        expect(screen.getByText('Test Connection 1')).toBeInTheDocument();
    });

    it('should display clear connection button when connection is selected', () => {
        const workflowTestConfigurationConnection = {
            connectionId: 1,
            workflowConnectionKey: 'connection_1',
            workflowNodeName: 'node-1',
        };

        render(
            <ConnectionTabConnectionSelect
                componentConnection={mockComponentConnection}
                componentConnectionsCount={1}
                componentDefinition={mockComponentDefinition}
                workflowId="workflow-1"
                workflowNodeName="node-1"
                workflowTestConfigurationConnection={workflowTestConfigurationConnection}
            />
        );

        const clearButton = screen.getByTitle('Clear connection');

        expect(clearButton).toBeInTheDocument();
    });

    it('should handle connection selection change', async () => {
        render(
            <ConnectionTabConnectionSelect
                componentConnection={mockComponentConnection}
                componentConnectionsCount={1}
                componentDefinition={mockComponentDefinition}
                workflowId="workflow-1"
                workflowNodeName="node-1"
            />
        );

        // Open select dropdown
        const selectTrigger = screen.getByRole('combobox');

        fireEvent.click(selectTrigger);

        // Select a connection
        const connection1Option = screen.getByText('Test Connection 1');

        fireEvent.click(connection1Option);

        await waitFor(() => {
            expect(mockSaveWorkflowTestConfigurationConnectionMutation).toHaveBeenCalledWith({
                connectionId: 1,
                environmentId: 1,
                workflowConnectionKey: 'connection_1',
                workflowId: 'workflow-1',
                workflowNodeName: 'node-1',
            });
        });
    });

    it('should display connection parameters when connection is selected', () => {
        const workflowTestConfigurationConnection = {
            connectionId: 1,
            workflowConnectionKey: 'connection_1',
            workflowNodeName: 'node-1',
        };

        render(
            <ConnectionTabConnectionSelect
                componentConnection={mockComponentConnection}
                componentConnectionsCount={1}
                componentDefinition={mockComponentDefinition}
                workflowId="workflow-1"
                workflowNodeName="node-1"
                workflowTestConfigurationConnection={workflowTestConfigurationConnection}
            />
        );

        expect(screen.getByTestId('connection-parameters')).toBeInTheDocument();
    });

    it('should show required mark when connection is required', () => {
        render(
            <ConnectionTabConnectionSelect
                componentConnection={mockComponentConnection}
                componentConnectionsCount={1}
                componentDefinition={mockComponentDefinition}
                workflowId="workflow-1"
                workflowNodeName="node-1"
            />
        );

        expect(screen.getByText('*')).toBeInTheDocument();
    });

    it('should not show create connection button when dialog is not allowed', () => {
        mockUseWorkflowNodeDetailsPanelStore.mockReturnValue({
            connectionDialogAllowed: false,
            currentComponent: {id: 'comp1'},
            currentNode: {id: 'node1'},
            setCurrentComponent: mockSetCurrentComponent,
            setCurrentNode: mockSetCurrentNode,
        });

        render(
            <ConnectionTabConnectionSelect
                componentConnection={mockComponentConnection}
                componentConnectionsCount={1}
                componentDefinition={mockComponentDefinition}
                workflowId="workflow-1"
                workflowNodeName="node-1"
            />
        );

        expect(screen.queryByTitle('Create a new connection')).not.toBeInTheDocument();
    });
});
