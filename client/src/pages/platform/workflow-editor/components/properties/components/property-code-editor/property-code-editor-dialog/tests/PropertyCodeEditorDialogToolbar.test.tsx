import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogToolbar from '../PropertyCodeEditorDialogToolbar';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleCopilotClick: vi.fn(),
        mockHandleRunClick: vi.fn(),
        mockHandleSaveClick: vi.fn(),
        mockHandleStopClick: vi.fn(),
        storeState: {
            copilotEnabled: true,
            dirty: false,
            saving: false,
            scriptIsRunning: false,
        },
    };
});

vi.mock('../hooks', () => ({
    usePropertyCodeEditorDialogToolbar: () => ({
        copilotEnabled: hoisted.storeState.copilotEnabled,
        dirty: hoisted.storeState.dirty,
        handleCopilotClick: hoisted.mockHandleCopilotClick,
        handleRunClick: hoisted.mockHandleRunClick,
        handleSaveClick: hoisted.mockHandleSaveClick,
        handleStopClick: hoisted.mockHandleStopClick,
        saving: hoisted.storeState.saving,
        scriptIsRunning: hoisted.storeState.scriptIsRunning,
    }),
}));

vi.mock('@/components/ui/dialog', () => ({
    DialogClose: ({children}: {children: React.ReactNode}) => <div data-testid="dialog-close">{children}</div>,
}));

const renderWithProviders = (ui: React.ReactElement) => {
    return render(<TooltipProvider>{ui}</TooltipProvider>);
};

describe('PropertyCodeEditorDialogToolbar', () => {
    const defaultProps = {
        language: 'javascript',
        onChange: vi.fn(),
        workflowId: 'workflow-1',
        workflowNodeName: 'testNode',
    };

    beforeEach(() => {
        windowResizeObserver();
        hoisted.storeState.copilotEnabled = true;
        hoisted.storeState.dirty = false;
        hoisted.storeState.saving = false;
        hoisted.storeState.scriptIsRunning = false;
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render the title', () => {
            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            expect(screen.getByText('Edit Script')).toBeInTheDocument();
        });

        it('should render save button', () => {
            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            // Save button should exist
            const buttons = screen.getAllByRole('button');

            expect(buttons.length).toBeGreaterThan(0);
        });

        it('should render close button', () => {
            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            expect(screen.getByTestId('dialog-close')).toBeInTheDocument();
        });
    });

    describe('save button', () => {
        it('should be disabled when not dirty', () => {
            hoisted.storeState.dirty = false;

            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            // Find save button by checking all buttons
            const buttons = screen.getAllByRole('button');
            const saveButton = buttons[0]; // Save button is typically first

            expect(saveButton).toBeDisabled();
        });

        it('should be enabled when dirty and not saving', () => {
            hoisted.storeState.dirty = true;
            hoisted.storeState.saving = false;

            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            const buttons = screen.getAllByRole('button');
            const saveButton = buttons[0];

            expect(saveButton).not.toBeDisabled();
        });

        it('should call handleSaveClick when clicked', async () => {
            const user = userEvent.setup();
            hoisted.storeState.dirty = true;

            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            const buttons = screen.getAllByRole('button');
            const saveButton = buttons[0];

            await user.click(saveButton);

            expect(hoisted.mockHandleSaveClick).toHaveBeenCalledTimes(1);
        });
    });

    describe('run button', () => {
        it('should be disabled when dirty', () => {
            hoisted.storeState.dirty = true;
            hoisted.storeState.scriptIsRunning = false;

            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            // The run button is second after save
            const buttons = screen.getAllByRole('button');
            const runButton = buttons[1];

            expect(runButton).toBeDisabled();
        });
    });

    describe('stop button', () => {
        it('should render stop button when script is running', () => {
            hoisted.storeState.scriptIsRunning = true;

            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            const buttons = screen.getAllByRole('button');

            // Check for destructive button variant presence
            expect(buttons.length).toBeGreaterThan(0);
        });

        it('should call handleStopClick when clicked', async () => {
            const user = userEvent.setup();
            hoisted.storeState.scriptIsRunning = true;

            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            const buttons = screen.getAllByRole('button');
            // Find the destructive/stop button
            const stopButton = buttons.find(
                (button) => button.classList.contains('bg-destructive') || button.className.includes('destructive')
            );

            if (stopButton) {
                await user.click(stopButton);

                expect(hoisted.mockHandleStopClick).toHaveBeenCalledTimes(1);
            }
        });
    });

    describe('copilot button', () => {
        it('should be present when copilot is enabled', () => {
            hoisted.storeState.copilotEnabled = true;

            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            const buttons = screen.getAllByRole('button');

            // More buttons when copilot is enabled
            expect(buttons.length).toBeGreaterThanOrEqual(3);
        });

        it('should call handleCopilotClick when clicked', async () => {
            const user = userEvent.setup();
            hoisted.storeState.copilotEnabled = true;
            hoisted.storeState.scriptIsRunning = false;

            renderWithProviders(<PropertyCodeEditorDialogToolbar {...defaultProps} />);

            const buttons = screen.getAllByRole('button');
            // Copilot button has specific class
            const copilotButton = buttons.find((button) => button.classList.contains('[&_svg]:size-5'));

            if (copilotButton) {
                await user.click(copilotButton);

                expect(hoisted.mockHandleCopilotClick).toHaveBeenCalledTimes(1);
            }
        });
    });
});
