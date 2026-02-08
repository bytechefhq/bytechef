import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialog from '../PropertyCodeEditorDialog';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleCopilotClose: vi.fn(),
        mockHandleOpenChange: vi.fn(),
        mockHandleUnsavedChangesAlertDialogCancel: vi.fn(),
        mockHandleUnsavedChangesAlertDialogClose: vi.fn(),
        storeState: {
            copilotPanelOpen: false,
            unsavedChangesAlertDialogOpen: false,
        },
    };
});

vi.mock('../hooks', () => ({
    usePropertyCodeEditorDialog: () => ({
        copilotPanelOpen: hoisted.storeState.copilotPanelOpen,
        handleCopilotClose: hoisted.mockHandleCopilotClose,
        handleOpenChange: hoisted.mockHandleOpenChange,
        handleUnsavedChangesAlertDialogCancel: hoisted.mockHandleUnsavedChangesAlertDialogCancel,
        handleUnsavedChangesAlertDialogClose: hoisted.mockHandleUnsavedChangesAlertDialogClose,
        unsavedChangesAlertDialogOpen: hoisted.storeState.unsavedChangesAlertDialogOpen,
    }),
}));

vi.mock('../PropertyCodeEditorDialogToolbar', () => ({
    default: () => <div data-testid="toolbar">Toolbar</div>,
}));

vi.mock('../PropertyCodeEditorDialogEditor', () => ({
    default: () => <div data-testid="editor">Editor</div>,
}));

vi.mock('../PropertyCodeEditorDialogExecutionOutput', () => ({
    default: () => <div data-testid="execution-output">Execution Output</div>,
}));

vi.mock('../PropertyCodeEditorDialogRightPanel', () => ({
    default: () => <div data-testid="right-panel">Right Panel</div>,
}));

vi.mock('@/shared/components/copilot/CopilotPanel', () => ({
    default: ({onClose}: {onClose: () => void}) => (
        <div data-testid="copilot-panel">
            Copilot Panel
            <button data-testid="copilot-close" onClick={onClose}>
                Close
            </button>
        </div>
    ),
}));

vi.mock('@/components/UnsavedChangesAlertDialog', () => ({
    default: ({onCancel, onClose, open}: {onCancel: () => void; onClose: () => void; open: boolean}) =>
        open ? (
            <div data-testid="unsaved-changes-dialog">
                <span>Unsaved Changes</span>

                <div>
                    <button data-testid="cancel-btn" onClick={onCancel}>
                        Cancel
                    </button>
                </div>

                <div>
                    <button data-testid="close-btn" onClick={onClose}>
                        Close
                    </button>
                </div>
            </div>
        ) : null,
}));

vi.mock('@/components/ui/resizable', () => ({
    ResizableHandle: () => <div data-testid="resizable-handle" />,
    ResizablePanel: ({children}: {children: React.ReactNode}) => <div data-testid="resizable-panel">{children}</div>,
    ResizablePanelGroup: ({children}: {children: React.ReactNode}) => (
        <div data-testid="resizable-panel-group">{children}</div>
    ),
}));

const renderWithProviders = (ui: React.ReactElement) => {
    return render(<TooltipProvider>{ui}</TooltipProvider>);
};

describe('PropertyCodeEditorDialog', () => {
    const defaultProps = {
        language: 'javascript',
        onChange: vi.fn(),
        onClose: vi.fn(),
        value: 'const x = 1;',
        workflow: {
            id: 'workflow-1',
            tasks: [],
            version: 1,
        },
        workflowNodeName: 'testNode',
    };

    beforeEach(() => {
        windowResizeObserver();
        hoisted.storeState.copilotPanelOpen = false;
        hoisted.storeState.unsavedChangesAlertDialogOpen = false;
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render the dialog with title', () => {
            renderWithProviders(<PropertyCodeEditorDialog {...defaultProps} />);

            expect(screen.getByText('Edit Script')).toBeInTheDocument();
        });

        it('should render the toolbar', () => {
            renderWithProviders(<PropertyCodeEditorDialog {...defaultProps} />);

            expect(screen.getByTestId('toolbar')).toBeInTheDocument();
        });

        it('should render the editor', () => {
            renderWithProviders(<PropertyCodeEditorDialog {...defaultProps} />);

            expect(screen.getByTestId('editor')).toBeInTheDocument();
        });

        it('should render the execution output', () => {
            renderWithProviders(<PropertyCodeEditorDialog {...defaultProps} />);

            expect(screen.getByTestId('execution-output')).toBeInTheDocument();
        });

        it('should render the right panel', () => {
            renderWithProviders(<PropertyCodeEditorDialog {...defaultProps} />);

            expect(screen.getByTestId('right-panel')).toBeInTheDocument();
        });

        it('should render resizable panels', () => {
            renderWithProviders(<PropertyCodeEditorDialog {...defaultProps} />);

            expect(screen.getByTestId('resizable-panel-group')).toBeInTheDocument();
            expect(screen.getAllByTestId('resizable-panel').length).toBeGreaterThan(0);
        });
    });

    describe('copilot panel', () => {
        it('should not render copilot panel when copilotPanelOpen is false', () => {
            hoisted.storeState.copilotPanelOpen = false;

            renderWithProviders(<PropertyCodeEditorDialog {...defaultProps} />);

            expect(screen.queryByTestId('copilot-panel')).not.toBeInTheDocument();
        });

        it('should render copilot panel when copilotPanelOpen is true', () => {
            hoisted.storeState.copilotPanelOpen = true;

            renderWithProviders(<PropertyCodeEditorDialog {...defaultProps} />);

            expect(screen.getByTestId('copilot-panel')).toBeInTheDocument();
        });
    });

    describe('unsaved changes dialog', () => {
        it('should not render unsaved changes dialog when closed', () => {
            hoisted.storeState.unsavedChangesAlertDialogOpen = false;

            renderWithProviders(<PropertyCodeEditorDialog {...defaultProps} />);

            expect(screen.queryByTestId('unsaved-changes-dialog')).not.toBeInTheDocument();
        });

        it('should render unsaved changes dialog when open', () => {
            hoisted.storeState.unsavedChangesAlertDialogOpen = true;

            renderWithProviders(<PropertyCodeEditorDialog {...defaultProps} />);

            expect(screen.getByTestId('unsaved-changes-dialog')).toBeInTheDocument();
        });
    });
});
