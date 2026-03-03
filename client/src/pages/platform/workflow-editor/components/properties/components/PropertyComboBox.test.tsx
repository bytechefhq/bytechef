import {render, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyComboBox from './PropertyComboBox';

const hoisted = vi.hoisted(() => ({
    mockClusterElementContext: vi.fn(),
    mockClusterElementOptionsQuery: vi.fn(),
    mockNodeDetailsPanelStore: vi.fn(),
    mockOnValueChange: vi.fn(),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: () => 1,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (...args: unknown[]) => hoisted.mockNodeDetailsPanelStore(...args),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: () => ({
        rootClusterElementNodeData: undefined,
    }),
}));

vi.mock('@/shared/queries/platform/workflowNodeOptions.queries', () => ({
    useGetClusterElementNodeOptionsQuery: () => ({
        data: undefined,
        isLoading: false,
        isRefetching: false,
    }),
    useGetWorkflowNodeOptionsQuery: () => ({
        data: undefined,
        isLoading: false,
        isRefetching: false,
    }),
}));

vi.mock('react-inlinesvg', () => ({
    default: () => null,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useClusterElementOptionsQuery: (...args: unknown[]) => hoisted.mockClusterElementOptionsQuery(...args),
}));

vi.mock('../ClusterElementContext', () => ({
    useClusterElementContext: () => hoisted.mockClusterElementContext(),
}));

beforeEach(() => {
    hoisted.mockOnValueChange.mockClear();

    hoisted.mockNodeDetailsPanelStore.mockReturnValue({
        currentNode: {
            connectionId: null,
            connections: [],
            workflowNodeName: 'test_1',
        },
        operationChangeInProgress: false,
    });

    hoisted.mockClusterElementContext.mockReturnValue(undefined);

    hoisted.mockClusterElementOptionsQuery.mockReturnValue({data: undefined, isLoading: false});
});

afterEach(() => {
    vi.clearAllMocks();
});

const defaultProps = {
    name: 'testCombo',
    onValueChange: hoisted.mockOnValueChange,
    options: [
        {description: 'First option description', label: 'Alpha', value: 'alpha'},
        {description: 'Second option description', label: 'Beta', value: 'beta'},
        {label: 'Gamma', value: 'gamma'},
    ],
    workflowId: 'workflow-1',
    workflowNodeName: 'test_1',
};

describe('PropertyComboBox', () => {
    it('should render the combobox with placeholder', () => {
        render(<PropertyComboBox {...defaultProps} />);

        expect(screen.getByRole('combobox')).toBeInTheDocument();
        expect(screen.getByText('Select...')).toBeInTheDocument();
    });

    it('should open the popover and display options when clicked', async () => {
        render(<PropertyComboBox {...defaultProps} />);

        await userEvent.click(screen.getByRole('combobox'));

        expect(screen.getByText('Alpha')).toBeInTheDocument();
        expect(screen.getByText('Beta')).toBeInTheDocument();
        expect(screen.getByText('Gamma')).toBeInTheDocument();
    });

    it('should filter options by label keyword when typing in the search input', async () => {
        render(<PropertyComboBox {...defaultProps} />);

        await userEvent.click(screen.getByRole('combobox'));

        const searchInput = screen.getByPlaceholderText('Search...');

        await userEvent.type(searchInput, 'Alpha');

        expect(screen.getByText('Alpha')).toBeInTheDocument();
        expect(screen.queryByText('Beta')).not.toBeInTheDocument();
        expect(screen.queryByText('Gamma')).not.toBeInTheDocument();
    });

    it('should filter options by description keyword when typing in the search input', async () => {
        render(<PropertyComboBox {...defaultProps} />);

        await userEvent.click(screen.getByRole('combobox'));

        const searchInput = screen.getByPlaceholderText('Search...');

        await userEvent.type(searchInput, 'Second option');

        expect(screen.queryByText('Alpha')).not.toBeInTheDocument();
        expect(screen.getByText('Beta')).toBeInTheDocument();
        expect(screen.queryByText('Gamma')).not.toBeInTheDocument();
    });

    it('should call onValueChange when an option is selected', async () => {
        render(<PropertyComboBox {...defaultProps} />);

        await userEvent.click(screen.getByRole('combobox'));
        await userEvent.click(screen.getByText('Beta'));

        expect(hoisted.mockOnValueChange).toHaveBeenCalledWith('beta');
    });

    it('should render a label when provided', () => {
        render(<PropertyComboBox {...defaultProps} label="Test Label" />);

        expect(screen.getByText('Test Label')).toBeInTheDocument();
    });

    it('should show the selected option label in the trigger', async () => {
        render(<PropertyComboBox {...defaultProps} value="alpha" />);

        expect(screen.getByText('Alpha')).toBeInTheDocument();
    });

    it('should handle null initial value without crashing', () => {
        // This tests the fix for null.toString() crash - uses != null instead of !== undefined
        expect(() => {
            render(<PropertyComboBox {...defaultProps} value={null as unknown as string} />);
        }).not.toThrow();
    });

    describe('cluster element context query', () => {
        const clusterElementContext = {
            clusterElementName: 'mcpTool_1',
            componentName: 'mcpServer',
            componentVersion: 1,
            connectionId: 42,
            inputParameters: {serverId: 'srv-1', toolName: 'search'},
        };

        const clusterElementOptions = [
            {description: 'Option from server', label: 'ServerOpt', value: 'serverOpt'},
            {label: 'ServerOpt2', value: 'serverOpt2'},
        ];

        const optionsDataSource = {
            optionsLookupDependsOn: ['serverId'],
        };

        beforeEach(() => {
            hoisted.mockNodeDetailsPanelStore.mockReturnValue({
                currentNode: undefined,
                operationChangeInProgress: false,
            });

            hoisted.mockClusterElementContext.mockReturnValue(clusterElementContext);
        });

        it('should render options from cluster element context query', async () => {
            hoisted.mockClusterElementOptionsQuery.mockReturnValue({
                data: {clusterElementOptions: clusterElementOptions},
                isLoading: false,
            });

            render(<PropertyComboBox {...defaultProps} options={[]} optionsDataSource={optionsDataSource} />);

            await userEvent.click(screen.getByRole('combobox'));

            expect(screen.getByText('ServerOpt')).toBeInTheDocument();
            expect(screen.getByText('ServerOpt2')).toBeInTheDocument();
        });

        it('should filter expression values from inputParameters', () => {
            hoisted.mockClusterElementContext.mockReturnValue({
                ...clusterElementContext,
                inputParameters: {
                    expressionField: '=fromAi(name)',
                    interpolatedField: '${some_var}',
                    normalField: 'plainValue',
                },
            });

            hoisted.mockClusterElementOptionsQuery.mockReturnValue({
                data: undefined,
                isLoading: false,
            });

            render(<PropertyComboBox {...defaultProps} options={[]} optionsDataSource={optionsDataSource} />);

            const queryCall = hoisted.mockClusterElementOptionsQuery.mock.calls[0];
            const queryArgs = queryCall[0];

            expect(queryArgs.inputParameters).toEqual({normalField: 'plainValue'});
        });

        it('should not enable query when currentNode is defined', () => {
            hoisted.mockNodeDetailsPanelStore.mockReturnValue({
                currentNode: {
                    connectionId: null,
                    connections: [],
                    workflowNodeName: 'test_1',
                },
                operationChangeInProgress: false,
            });

            hoisted.mockClusterElementOptionsQuery.mockReturnValue({
                data: undefined,
                isLoading: false,
            });

            render(<PropertyComboBox {...defaultProps} options={[]} optionsDataSource={optionsDataSource} />);

            const queryCall = hoisted.mockClusterElementOptionsQuery.mock.calls[0];
            const queryOptions = queryCall[1];

            expect(queryOptions.enabled).toBe(false);
        });

        it('should not enable query when dependency values contain undefined', () => {
            hoisted.mockClusterElementContext.mockReturnValue({
                ...clusterElementContext,
                inputParameters: {},
            });

            hoisted.mockClusterElementOptionsQuery.mockReturnValue({
                data: undefined,
                isLoading: false,
            });

            render(
                <PropertyComboBox
                    {...defaultProps}
                    lookupDependsOnPaths={['serverId']}
                    lookupDependsOnValues={[undefined]}
                    options={[]}
                    optionsDataSource={optionsDataSource}
                />
            );

            const queryCall = hoisted.mockClusterElementOptionsQuery.mock.calls[0];
            const queryOptions = queryCall[1];

            expect(queryOptions.enabled).toBe(false);
        });

        it('should not enable query when no optionsDataSource is provided', () => {
            hoisted.mockClusterElementOptionsQuery.mockReturnValue({
                data: undefined,
                isLoading: false,
            });

            render(<PropertyComboBox {...defaultProps} options={[]} />);

            const queryCall = hoisted.mockClusterElementOptionsQuery.mock.calls[0];
            const queryOptions = queryCall[1];

            expect(queryOptions.enabled).toBe(false);
        });

        it('should enable query when all conditions are met', () => {
            hoisted.mockClusterElementOptionsQuery.mockReturnValue({
                data: undefined,
                isLoading: false,
            });

            render(
                <PropertyComboBox
                    {...defaultProps}
                    lookupDependsOnPaths={['serverId']}
                    lookupDependsOnValues={['srv-1']}
                    options={[]}
                    optionsDataSource={optionsDataSource}
                />
            );

            const queryCall = hoisted.mockClusterElementOptionsQuery.mock.calls[0];
            const queryOptions = queryCall[1];

            expect(queryOptions.enabled).toBe(true);
        });

        it('should fall back to initialOptions when no query data is available', async () => {
            hoisted.mockClusterElementOptionsQuery.mockReturnValue({
                data: undefined,
                isLoading: false,
            });

            render(<PropertyComboBox {...defaultProps} optionsDataSource={optionsDataSource} />);

            await userEvent.click(screen.getByRole('combobox'));

            expect(screen.getByText('Alpha')).toBeInTheDocument();
            expect(screen.getByText('Beta')).toBeInTheDocument();
            expect(screen.getByText('Gamma')).toBeInTheDocument();
        });
    });
});
