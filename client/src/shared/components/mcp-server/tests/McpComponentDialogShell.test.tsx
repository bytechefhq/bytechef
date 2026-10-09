import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentDialogShell from '../McpComponentDialogShell';

const onBack = vi.fn();
const onOpenChange = vi.fn();
const onSave = vi.fn();

const defaultProps = {
    children: <div data-testid="step" />,
    currentStep: 'components' as const,
    editing: false,
    onBack,
    onOpenChange,
    onSave,
    open: true,
    selectedComponent: {name: 'slack', title: 'Slack'},
    selectedToolCount: 0,
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpComponentDialogShell', () => {
    describe('header copy', () => {
        it('should name the component step', () => {
            render(<McpComponentDialogShell {...defaultProps} />);

            expect(screen.getByText('Select Component')).toBeInTheDocument();
            expect(screen.getByText('Choose a component to add to your MCP server.')).toBeInTheDocument();
        });

        it('should name the tool step when adding a component', () => {
            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" />);

            expect(screen.getByText('Select Tools from Slack')).toBeInTheDocument();
            expect(screen.getByText('Select the tools you want to enable for this component.')).toBeInTheDocument();
        });

        it('should name the tool step when editing a component', () => {
            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" editing />);

            expect(screen.getByText('Edit Tools for Slack')).toBeInTheDocument();
            expect(screen.getByText('Modify the tools enabled for this component.')).toBeInTheDocument();
        });

        // The automation side selects a REST definition and the embedded side a GraphQL one, so the label has to
        // tolerate a missing title on either.
        it('should fall back to the component name when it has no title', () => {
            render(
                <McpComponentDialogShell {...defaultProps} currentStep="tools" selectedComponent={{name: 'slack'}} />
            );

            expect(screen.getByText('Select Tools from slack')).toBeInTheDocument();
        });
    });

    describe('step content', () => {
        it('should render the given step', () => {
            render(<McpComponentDialogShell {...defaultProps} />);

            expect(screen.getByTestId('step')).toBeInTheDocument();
        });

        // Asserted closed on purpose: an open Radix dialog marks everything outside its portal aria-hidden, which
        // hides the trigger from role queries. Closed is also the only state the trigger is useful in.
        it('should render a trigger node when one is given', () => {
            render(
                <McpComponentDialogShell
                    {...defaultProps}
                    open={false}
                    triggerNode={<button type="button">Add Component</button>}
                />
            );

            expect(screen.getByRole('button', {name: 'Add Component'})).toBeInTheDocument();
        });

        it('should not reserve a trigger when none is given', () => {
            render(<McpComponentDialogShell {...defaultProps} open={false} />);

            expect(screen.queryByRole('button')).not.toBeInTheDocument();
        });

        it('should not render when closed', () => {
            render(<McpComponentDialogShell {...defaultProps} open={false} />);

            expect(screen.queryByText('Select Component')).not.toBeInTheDocument();
        });
    });

    describe('footer', () => {
        it('should offer no actions on the component step', () => {
            render(<McpComponentDialogShell {...defaultProps} />);

            expect(screen.queryByRole('button', {name: 'Save'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();
        });

        it('should pluralize the tool count', () => {
            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" selectedToolCount={2} />);

            expect(screen.getByText('2 tools selected')).toBeInTheDocument();
        });

        it('should not pluralize a single tool', () => {
            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" selectedToolCount={1} />);

            expect(screen.getByText('1 tool selected')).toBeInTheDocument();
        });

        it('should place the tool count apart from the actions', () => {
            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" selectedToolCount={2} />);

            const toolCount = screen.getByText('2 tools selected');
            const footer = toolCount.closest('[data-slot="dialog-footer"]');

            expect(footer?.firstElementChild).toContainElement(toolCount);
            expect(footer?.lastElementChild).toContainElement(screen.getByRole('button', {name: 'Save'}));
        });

        it('should offer Back when adding a component', () => {
            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" />);

            expect(screen.getByRole('button', {name: 'Back'})).toBeInTheDocument();
        });

        it('should not offer Back when editing a component', () => {
            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" editing />);

            expect(screen.queryByRole('button', {name: 'Back'})).not.toBeInTheDocument();
        });

        it('should disable saving until a tool is selected', () => {
            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" />);

            expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
        });

        it('should label the action Update when editing', () => {
            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" editing selectedToolCount={1} />);

            expect(screen.getByRole('button', {name: 'Update'})).toBeEnabled();
        });
    });

    describe('wiring', () => {
        it('should go back when Back is clicked', async () => {
            const user = userEvent.setup();

            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" />);

            await user.click(screen.getByRole('button', {name: 'Back'}));

            expect(onBack).toHaveBeenCalledTimes(1);
        });

        it('should save when the action is clicked', async () => {
            const user = userEvent.setup();

            render(<McpComponentDialogShell {...defaultProps} currentStep="tools" selectedToolCount={1} />);

            await user.click(screen.getByRole('button', {name: 'Save'}));

            expect(onSave).toHaveBeenCalledTimes(1);
        });

        it('should close when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<McpComponentDialogShell {...defaultProps} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onOpenChange).toHaveBeenCalledWith(false);
        });
    });
});
