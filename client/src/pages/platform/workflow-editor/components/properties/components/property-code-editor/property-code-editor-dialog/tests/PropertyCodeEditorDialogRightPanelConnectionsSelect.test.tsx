import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogRightPanelConnectionsSelect from '../PropertyCodeEditorDialogRightPanelConnectionsSelect';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleValueChange: vi.fn(),
        mockSetShowNewConnectionDialog: vi.fn(),
        mockUseCreateConnectionMutation: vi.fn(),
        mockUseGetConnectionTagsQuery: vi.fn(),
        storeState: {
            componentDefinition: {
                connection: {version: 1},
                name: 'slack',
                title: 'Slack',
                version: 1,
            },
            componentDefinitions: [{name: 'slack', title: 'Slack', version: 1}],
            connectionId: undefined as number | undefined,
            connections: [
                {environmentId: 1, id: 1, name: 'Slack Connection 1', tags: [{name: 'Production'}]},
                {environmentId: 2, id: 2, name: 'Slack Connection 2', tags: [{name: 'Development'}]},
            ],
            showNewConnectionDialog: false,
        },
    };
});

vi.mock('../hooks/usePropertyCodeEditorDialogRightPanelConnectionsSelect', () => ({
    default: () => ({
        ConnectionKeys: {
            connectionTags: ['connectionTags'],
            connections: ['connections'],
        },
        componentDefinition: hoisted.storeState.componentDefinition,
        componentDefinitions: hoisted.storeState.componentDefinitions,
        connectionId: hoisted.storeState.connectionId,
        connections: hoisted.storeState.connections,
        handleValueChange: hoisted.mockHandleValueChange,
        setShowNewConnectionDialog: hoisted.mockSetShowNewConnectionDialog,
        showNewConnectionDialog: hoisted.storeState.showNewConnectionDialog,
        useCreateConnectionMutation: hoisted.mockUseCreateConnectionMutation,
        useGetConnectionTagsQuery: hoisted.mockUseGetConnectionTagsQuery,
    }),
}));

vi.mock('@/components/ui/select', () => ({
    Select: ({
        children,
        onValueChange,
        value,
    }: {
        children: React.ReactNode;
        onValueChange: (value: string) => void;
        required?: boolean;
        value?: string;
    }) => (
        <div data-testid="select" data-value={value}>
            {children}

            <button data-testid="trigger-change" onClick={() => onValueChange('1')}>
                Trigger Change
            </button>
        </div>
    ),
    SelectContent: ({children}: {children: React.ReactNode}) => <div data-testid="select-content">{children}</div>,
    SelectItem: ({children, value}: {children: React.ReactNode; value: string}) => (
        <div data-testid={`select-item-${value}`} data-value={value}>
            {children}
        </div>
    ),
    SelectTrigger: ({children}: {children: React.ReactNode}) => <div data-testid="select-trigger">{children}</div>,
    SelectValue: ({placeholder}: {placeholder: string}) => <span data-testid="select-value">{placeholder}</span>,
}));

vi.mock('@/shared/components/EnvironmentBadge', () => ({
    default: ({environmentId}: {environmentId: number}) => (
        <span data-testid={`env-badge-${environmentId}`}>Env {environmentId}</span>
    ),
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: ({onClose}: {onClose: () => void}) => (
        <div data-testid="connection-dialog">
            Connection Dialog
            <button data-testid="close-dialog-btn" onClick={onClose}>
                Close
            </button>
        </div>
    ),
}));

describe('PropertyCodeEditorDialogRightPanelConnectionsSelect', () => {
    const defaultProps = {
        componentConnection: {
            componentName: 'slack',
            componentVersion: 1,
            key: 'slack_1',
            required: true,
            workflowNodeName: 'testNode',
        },
        workflowId: 'workflow-1',
        workflowNodeName: 'testNode',
        workflowTestConfigurationConnection: undefined,
    };

    beforeEach(() => {
        windowResizeObserver();
        hoisted.storeState.connectionId = undefined;
        hoisted.storeState.showNewConnectionDialog = false;
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render the select component', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByTestId('select')).toBeInTheDocument();
        });

        it('should render the select trigger', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByTestId('select-trigger')).toBeInTheDocument();
        });

        it('should render placeholder', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByText('Choose Connection...')).toBeInTheDocument();
        });

        it('should render create new connection button', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByTitle('Create a new connection')).toBeInTheDocument();
        });
    });

    describe('connections list', () => {
        it('should render connection items', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByTestId('select-item-1')).toBeInTheDocument();
            expect(screen.getByTestId('select-item-2')).toBeInTheDocument();
        });

        it('should display connection names', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByText('Slack Connection 1')).toBeInTheDocument();
            expect(screen.getByText('Slack Connection 2')).toBeInTheDocument();
        });

        it('should display connection tags', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByText('Production')).toBeInTheDocument();
            expect(screen.getByText('Development')).toBeInTheDocument();
        });

        it('should render environment badges', () => {
            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByTestId('env-badge-1')).toBeInTheDocument();
            expect(screen.getByTestId('env-badge-2')).toBeInTheDocument();
        });
    });

    describe('value change', () => {
        it('should call handleValueChange when value changes', async () => {
            const user = userEvent.setup();

            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            await user.click(screen.getByTestId('trigger-change'));

            expect(hoisted.mockHandleValueChange).toHaveBeenCalledWith(1, 'slack_1');
        });
    });

    describe('create new connection button', () => {
        it('should call setShowNewConnectionDialog when clicked', async () => {
            const user = userEvent.setup();

            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            await user.click(screen.getByTitle('Create a new connection'));

            expect(hoisted.mockSetShowNewConnectionDialog).toHaveBeenCalledWith(true);
        });
    });

    describe('connection dialog', () => {
        it('should render connection dialog when showNewConnectionDialog is true', () => {
            hoisted.storeState.showNewConnectionDialog = true;

            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByTestId('connection-dialog')).toBeInTheDocument();
        });

        it('should not render connection dialog when showNewConnectionDialog is false', () => {
            hoisted.storeState.showNewConnectionDialog = false;

            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.queryByTestId('connection-dialog')).not.toBeInTheDocument();
        });

        it('should call setShowNewConnectionDialog(false) when dialog is closed', async () => {
            const user = userEvent.setup();
            hoisted.storeState.showNewConnectionDialog = true;

            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            await user.click(screen.getByTestId('close-dialog-btn'));

            expect(hoisted.mockSetShowNewConnectionDialog).toHaveBeenCalledWith(false);
        });
    });

    describe('selected value', () => {
        it('should show selected connection ID', () => {
            hoisted.storeState.connectionId = 1;

            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByTestId('select')).toHaveAttribute('data-value', '1');
        });

        it('should show undefined value when no connection selected', () => {
            hoisted.storeState.connectionId = undefined;

            render(<PropertyCodeEditorDialogRightPanelConnectionsSelect {...defaultProps} />);

            expect(screen.getByTestId('select')).not.toHaveAttribute('data-value', '1');
        });
    });
});
