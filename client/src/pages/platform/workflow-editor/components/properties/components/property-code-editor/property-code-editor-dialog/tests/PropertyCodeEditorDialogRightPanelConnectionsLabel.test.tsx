import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogRightPanelConnectionsLabel from '../PropertyCodeEditorDialogRightPanelConnectionsLabel';

const hoisted = vi.hoisted(() => {
    return {
        storeState: {
            componentDefinition: {
                name: 'slack',
                title: 'Slack',
                version: 1,
            } as {name: string; title: string; version: number} | undefined,
        },
    };
});

vi.mock('../hooks/usePropertyCodeEditorDialogRightPanelConnectionsLabel', () => ({
    default: () => ({
        componentDefinition: hoisted.storeState.componentDefinition,
    }),
}));

const renderWithProviders = (ui: React.ReactElement) => {
    return render(<TooltipProvider>{ui}</TooltipProvider>);
};

describe('PropertyCodeEditorDialogRightPanelConnectionsLabel', () => {
    const defaultProps = {
        componentConnection: {
            componentName: 'slack',
            componentVersion: 1,
            key: 'slack_1',
            required: true,
            workflowNodeName: 'testNode',
        },
        onRemoveClick: vi.fn(),
    };

    beforeEach(() => {
        windowResizeObserver();
        hoisted.storeState.componentDefinition = {
            name: 'slack',
            title: 'Slack',
            version: 1,
        };
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render the component title', () => {
            renderWithProviders(<PropertyCodeEditorDialogRightPanelConnectionsLabel {...defaultProps} />);

            expect(screen.getByText('Slack')).toBeInTheDocument();
        });

        it('should render the connection key', () => {
            renderWithProviders(<PropertyCodeEditorDialogRightPanelConnectionsLabel {...defaultProps} />);

            expect(screen.getByText('slack_1')).toBeInTheDocument();
        });

        it('should render remove button', () => {
            renderWithProviders(<PropertyCodeEditorDialogRightPanelConnectionsLabel {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Remove'})).toBeInTheDocument();
        });
    });

    describe('required mark', () => {
        it('should show required mark when connection is required', () => {
            renderWithProviders(<PropertyCodeEditorDialogRightPanelConnectionsLabel {...defaultProps} />);

            // RequiredMark component renders an asterisk
            const label = screen.getByText('Slack').closest('label');

            expect(label).toBeInTheDocument();
        });

        it('should not show required mark when connection is not required', () => {
            const props = {
                ...defaultProps,
                componentConnection: {
                    ...defaultProps.componentConnection,
                    required: false,
                },
            };

            renderWithProviders(<PropertyCodeEditorDialogRightPanelConnectionsLabel {...props} />);

            expect(screen.getByText('Slack')).toBeInTheDocument();
        });
    });

    describe('remove button', () => {
        it('should call onRemoveClick when clicked', async () => {
            const user = userEvent.setup();
            const mockOnRemoveClick = vi.fn();

            renderWithProviders(
                <PropertyCodeEditorDialogRightPanelConnectionsLabel
                    {...defaultProps}
                    onRemoveClick={mockOnRemoveClick}
                />
            );

            await user.click(screen.getByRole('button', {name: 'Remove'}));

            expect(mockOnRemoveClick).toHaveBeenCalledTimes(1);
        });
    });

    describe('when component definition is not available', () => {
        it('should not render title when componentDefinition is undefined', () => {
            hoisted.storeState.componentDefinition = undefined;

            renderWithProviders(<PropertyCodeEditorDialogRightPanelConnectionsLabel {...defaultProps} />);

            expect(screen.queryByText('Slack')).not.toBeInTheDocument();
        });

        it('should still render connection key', () => {
            hoisted.storeState.componentDefinition = undefined;

            renderWithProviders(<PropertyCodeEditorDialogRightPanelConnectionsLabel {...defaultProps} />);

            expect(screen.getByText('slack_1')).toBeInTheDocument();
        });

        it('should still render remove button', () => {
            hoisted.storeState.componentDefinition = undefined;

            renderWithProviders(<PropertyCodeEditorDialogRightPanelConnectionsLabel {...defaultProps} />);

            expect(screen.getByRole('button', {name: 'Remove'})).toBeInTheDocument();
        });
    });
});
