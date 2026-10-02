import {McpComponent} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentDialog from '../McpComponentDialog';

const hoisted = vi.hoisted(() => ({
    handleOpenChange: vi.fn(),
    handleSave: vi.fn(),
    state: {
        currentStep: 'components' as 'components' | 'tools',
        selectedTools: [] as unknown[],
    },
}));

vi.mock('../hooks/useMcpComponentDialog', () => ({
    default: () => ({
        currentStep: hoisted.state.currentStep,
        existingTools: [],
        handleBack: vi.fn(),
        handleClose: vi.fn(),
        handleComponentSelect: vi.fn(),
        handleOpenChange: hoisted.handleOpenChange,
        handleSave: hoisted.handleSave,
        selectedComponent: undefined,
        selectedTools: hoisted.state.selectedTools,
        setSelectedTools: vi.fn(),
    }),
}));

vi.mock('../McpComponentDialogComponentSelectionStep', () => ({
    default: () => <div data-testid="component-selection-step" />,
}));

vi.mock('../McpComponentDialogToolSelectionStep', () => ({
    default: () => <div data-testid="tool-selection-step" />,
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.state.currentStep = 'components';
    hoisted.state.selectedTools = [];
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpComponentDialog', () => {
    describe('component selection step', () => {
        it('should render the component step title and description', () => {
            render(<McpComponentDialog mcpServerId="1" open />);

            expect(screen.getByText('Select Component')).toBeInTheDocument();
            expect(screen.getByText('Choose a component to add to your MCP server.')).toBeInTheDocument();
        });

        it('should render the component selection step', () => {
            render(<McpComponentDialog mcpServerId="1" open />);

            expect(screen.getByTestId('component-selection-step')).toBeInTheDocument();
        });
    });

    describe('tool selection step', () => {
        beforeEach(() => {
            hoisted.state.currentStep = 'tools';
        });

        it('should render the tool step description when adding a component', () => {
            render(<McpComponentDialog mcpServerId="1" open />);

            expect(screen.getByText('Select the tools you want to enable for this component.')).toBeInTheDocument();
        });

        it('should render the edit description when a component is given', () => {
            render(<McpComponentDialog mcpComponent={{id: '1'} as McpComponent} mcpServerId="1" open />);

            expect(screen.getByText('Modify the tools enabled for this component.')).toBeInTheDocument();
        });

        it('should render the tool selection step', () => {
            render(<McpComponentDialog mcpServerId="1" open />);

            expect(screen.getByTestId('tool-selection-step')).toBeInTheDocument();
        });

        it('should offer Back when adding a new component', () => {
            render(<McpComponentDialog mcpServerId="1" open />);

            expect(screen.getByRole('button', {name: 'Back'})).toBeInTheDocument();
        });

        it('should not offer Back when editing an existing component', () => {
            render(<McpComponentDialog mcpComponent={{id: '1'} as McpComponent} mcpServerId="1" open />);

            expect(screen.queryByRole('button', {name: 'Back'})).not.toBeInTheDocument();
        });

        it('should disable saving until a tool is selected', () => {
            render(<McpComponentDialog mcpServerId="1" open />);

            expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
        });

        it('should enable saving once a tool is selected', () => {
            hoisted.state.selectedTools = [{name: 'tool'}];

            render(<McpComponentDialog mcpServerId="1" open />);

            expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled();
        });

        it('should label the action Update when editing', () => {
            render(<McpComponentDialog mcpComponent={{id: '1'} as McpComponent} mcpServerId="1" open />);

            expect(screen.getByRole('button', {name: 'Update'})).toBeInTheDocument();
        });

        it('should save when the action is clicked', async () => {
            const user = userEvent.setup();

            hoisted.state.selectedTools = [{name: 'tool'}];

            render(<McpComponentDialog mcpServerId="1" open />);

            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(hoisted.handleSave).toHaveBeenCalledTimes(1);
        });
    });

    describe('dialog chrome', () => {
        it('should render the close and cancel controls', () => {
            hoisted.state.currentStep = 'tools';

            render(<McpComponentDialog mcpServerId="1" open />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        });

        it('should close the dialog when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<McpComponentDialog mcpServerId="1" open />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(hoisted.handleOpenChange).toHaveBeenCalledWith(false);
        });

        it('should not render when closed', () => {
            render(<McpComponentDialog mcpServerId="1" open={false} />);

            expect(screen.queryByText('Select Component')).not.toBeInTheDocument();
        });
    });
});
