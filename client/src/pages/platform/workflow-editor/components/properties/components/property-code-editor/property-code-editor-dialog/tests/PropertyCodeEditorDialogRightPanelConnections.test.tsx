import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogRightPanelConnections from '../PropertyCodeEditorDialogRightPanelConnections';

const hoisted = vi.hoisted(() => {
    return {
        mockHandleCloseConnectionNote: vi.fn(),
        mockHandleOnRemoveClick: vi.fn(),
        mockHandleOnSubmit: vi.fn(),
        mockSetShowNewConnectionDialog: vi.fn(),
        mockUseCreateConnectionMutation: vi.fn(),
        mockUseGetConnectionTagsQuery: vi.fn(),
        storeState: {
            componentDefinitions: [{name: 'slack', title: 'Slack', version: 1}],
            showConnectionNote: true,
            showNewConnectionDialog: false,
            workflowTestConfigurationConnections: [] as Array<{connectionId: number; workflowConnectionKey: string}>,
        },
    };
});

vi.mock('../hooks/usePropertyCodeEditorDialogRightPanelConnections', () => ({
    usePropertyCodeEditorDialogRightPanelConnections: () => ({
        ConnectionKeys: {
            connectionTags: ['connectionTags'],
            connections: ['connections'],
        },
        componentDefinitions: hoisted.storeState.componentDefinitions,
        handleCloseConnectionNote: hoisted.mockHandleCloseConnectionNote,
        handleOnRemoveClick: hoisted.mockHandleOnRemoveClick,
        handleOnSubmit: hoisted.mockHandleOnSubmit,
        setShowNewConnectionDialog: hoisted.mockSetShowNewConnectionDialog,
        showConnectionNote: hoisted.storeState.showConnectionNote,
        showNewConnectionDialog: hoisted.storeState.showNewConnectionDialog,
        useCreateConnectionMutation: hoisted.mockUseCreateConnectionMutation,
        useGetConnectionTagsQuery: hoisted.mockUseGetConnectionTagsQuery,
        workflowTestConfigurationConnections: hoisted.storeState.workflowTestConfigurationConnections,
    }),
}));

vi.mock('../PropertyCodeEditorDialogRightPanelConnectionsLabel', () => ({
    default: ({
        componentConnection,
        onRemoveClick,
    }: {
        componentConnection: {key: string};
        onRemoveClick: () => void;
    }) => (
        <div data-testid={`connection-label-${componentConnection.key}`}>
            <span>{componentConnection.key}</span>

            <button data-testid={`remove-btn-${componentConnection.key}`} onClick={onRemoveClick}>
                Remove
            </button>
        </div>
    ),
}));

vi.mock('../PropertyCodeEditorDialogRightPanelConnectionsSelect', () => ({
    default: ({componentConnection}: {componentConnection: {key: string}}) => (
        <div data-testid={`connection-select-${componentConnection.key}`}>Select for {componentConnection.key}</div>
    ),
}));

vi.mock('../PropertyCodeEditorDialogRightPanelConnectionsPopover', () => ({
    default: ({onSubmit, triggerNode}: {onSubmit: (values: unknown) => void; triggerNode?: React.ReactNode}) => (
        <div data-testid="connections-popover">
            {triggerNode || <button data-testid="add-component-btn">Add Component</button>}

            <button
                data-testid="submit-popover-btn"
                onClick={() => onSubmit({componentName: 'slack', componentVersion: 1, name: 'mySlack'})}
            >
                Submit
            </button>
        </div>
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

describe('PropertyCodeEditorDialogRightPanelConnections', () => {
    const defaultProps = {
        componentConnections: [
            {componentName: 'slack', componentVersion: 1, key: 'slack_1', required: true, workflowNodeName: 'testNode'},
            {
                componentName: 'github',
                componentVersion: 1,
                key: 'github_1',
                required: false,
                workflowNodeName: 'testNode',
            },
        ],
        workflow: {
            definition: '{}',
            id: 'workflow-1',
            tasks: [],
            version: 1,
        },
        workflowNodeName: 'testNode',
    };

    beforeEach(() => {
        windowResizeObserver();
        hoisted.storeState.showConnectionNote = true;
        hoisted.storeState.showNewConnectionDialog = false;
        hoisted.storeState.workflowTestConfigurationConnections = [];
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render the Connections title', () => {
            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} />);

            expect(screen.getByText('Connections')).toBeInTheDocument();
        });
    });

    describe('when component connections exist', () => {
        it('should render connection labels for each connection', () => {
            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} />);

            expect(screen.getByTestId('connection-label-slack_1')).toBeInTheDocument();
            expect(screen.getByTestId('connection-label-github_1')).toBeInTheDocument();
        });

        it('should render connection selects for each connection', () => {
            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} />);

            expect(screen.getByTestId('connection-select-slack_1')).toBeInTheDocument();
            expect(screen.getByTestId('connection-select-github_1')).toBeInTheDocument();
        });

        it('should render add component popover', () => {
            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} />);

            expect(screen.getByTestId('connections-popover')).toBeInTheDocument();
        });

        it('should call handleOnRemoveClick when remove button is clicked', async () => {
            const user = userEvent.setup();

            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} />);

            await user.click(screen.getByTestId('remove-btn-slack_1'));

            expect(hoisted.mockHandleOnRemoveClick).toHaveBeenCalledWith('slack_1');
        });

        it('should call handleOnSubmit when popover form is submitted', async () => {
            const user = userEvent.setup();

            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} />);

            await user.click(screen.getByTestId('submit-popover-btn'));

            expect(hoisted.mockHandleOnSubmit).toHaveBeenCalledWith({
                componentName: 'slack',
                componentVersion: 1,
                name: 'mySlack',
            });
        });
    });

    describe('when no component connections exist', () => {
        it('should display empty state message', () => {
            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} componentConnections={[]} />);

            expect(screen.getByText('No defined components')).toBeInTheDocument();
            expect(
                screen.getByText('You have not defined any component and its connection to use inside this script yet.')
            ).toBeInTheDocument();
        });

        it('should display add component button', () => {
            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} componentConnections={[]} />);

            expect(screen.getByTestId('connections-popover')).toBeInTheDocument();
        });

        it('should display connection note when showConnectionNote is true', () => {
            hoisted.storeState.showConnectionNote = true;

            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} componentConnections={[]} />);

            expect(screen.getByText('Note')).toBeInTheDocument();
            expect(
                screen.getByText('The selected connections are used for testing purposes only.')
            ).toBeInTheDocument();
        });

        it('should not display connection note when showConnectionNote is false', () => {
            hoisted.storeState.showConnectionNote = false;

            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} componentConnections={[]} />);

            expect(screen.queryByText('Note')).not.toBeInTheDocument();
        });

        it('should call handleCloseConnectionNote when note close button is clicked', async () => {
            const user = userEvent.setup();
            hoisted.storeState.showConnectionNote = true;

            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} componentConnections={[]} />);

            const closeNoteButton = screen.getByTitle('Close the note');

            await user.click(closeNoteButton);

            expect(hoisted.mockHandleCloseConnectionNote).toHaveBeenCalled();
        });
    });

    describe('connection dialog', () => {
        it('should render connection dialog when showNewConnectionDialog is true', () => {
            hoisted.storeState.showNewConnectionDialog = true;

            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} />);

            expect(screen.getByTestId('connection-dialog')).toBeInTheDocument();
        });

        it('should not render connection dialog when showNewConnectionDialog is false', () => {
            hoisted.storeState.showNewConnectionDialog = false;

            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} />);

            expect(screen.queryByTestId('connection-dialog')).not.toBeInTheDocument();
        });

        it('should call setShowNewConnectionDialog(false) when dialog is closed', async () => {
            const user = userEvent.setup();
            hoisted.storeState.showNewConnectionDialog = true;

            render(<PropertyCodeEditorDialogRightPanelConnections {...defaultProps} />);

            await user.click(screen.getByTestId('close-dialog-btn'));

            expect(hoisted.mockSetShowNewConnectionDialog).toHaveBeenCalledWith(false);
        });
    });
});
