import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import CodeWorkflowReferencesDialog from './CodeWorkflowReferencesDialog';

const hoisted = vi.hoisted(() => ({
    mockOnClose: vi.fn(),
    mockUseConnectedUserCodeWorkflowReferencesQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useConnectedUserCodeWorkflowReferencesQuery: hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery,
    };
});

vi.mock('@/components/ui/tooltip', () => ({
    Tooltip: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    TooltipContent: ({children}: {children: React.ReactNode}) => <div data-testid="tooltip-content">{children}</div>,
    TooltipTrigger: ({children}: {children: React.ReactNode}) => <div data-testid="tooltip-trigger">{children}</div>,
}));

const workflowTemplates = [{label: 'My Workflow', workflowUuid: 'uuid-1'}];

const renderDialog = () =>
    render(
        <CodeWorkflowReferencesDialog
            onClose={hoisted.mockOnClose}
            projectName="My Project"
            workflowTemplates={workflowTemplates}
        />
    );

afterEach(() => {
    resetAll();
});

describe('CodeWorkflowReferencesDialog', () => {
    it('shows a loading indicator while the references query is pending', () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: undefined,
            error: null,
            isLoading: true,
        });

        renderDialog();

        expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('shows an empty state when no connected user references the project', () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: {connectedUserCodeWorkflowReferences: []},
            error: null,
            isLoading: false,
        });

        renderDialog();

        expect(screen.getByText(/no connected user has referenced this project yet/i)).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('renders a dangling badge with the dangling reason for a dangling reference', () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: {
                connectedUserCodeWorkflowReferences: [
                    {
                        catalogWorkflowUuid: 'uuid-1',
                        dangling: true,
                        danglingReason: 'Workflow removed on redeploy',
                        enabled: false,
                        externalUserId: 'ext-1',
                    },
                ],
            },
            error: null,
            isLoading: false,
        });

        renderDialog();

        expect(screen.getByText('ext-1')).toBeInTheDocument();
        expect(screen.getByText('Dangling')).toBeInTheDocument();
        expect(screen.getByText('Workflow removed on redeploy')).toBeInTheDocument();
        expect(screen.getByText('Disabled')).toBeInTheDocument();
    });

    it('renders the panel as read-only, with no controls to mutate a reference', () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: {
                connectedUserCodeWorkflowReferences: [
                    {
                        catalogWorkflowUuid: 'uuid-1',
                        dangling: false,
                        danglingReason: null,
                        enabled: true,
                        externalUserId: 'ext-1',
                    },
                ],
            },
            error: null,
            isLoading: false,
        });

        renderDialog();

        expect(screen.getByText('ext-1')).toBeInTheDocument();
        expect(screen.getByText('Enabled')).toBeInTheDocument();
        expect(screen.queryAllByRole('button')).toHaveLength(1);
        expect(screen.getByRole('button', {name: /close/i})).toBeInTheDocument();
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        expect(screen.getByText(/this list is read-only/i)).toBeInTheDocument();
    });
});
