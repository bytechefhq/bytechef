import {render, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyComboBox from './PropertyComboBox';

const hoisted = vi.hoisted(() => ({
    mockOnValueChange: vi.fn(),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: () => 1,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: () => ({
        currentNode: {
            connectionId: null,
            connections: [],
            workflowNodeName: 'test_1',
        },
        operationChangeInProgress: false,
    }),
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

beforeEach(() => {
    hoisted.mockOnValueChange.mockClear();
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
});
