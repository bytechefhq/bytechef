import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectUsageSheet from './AutomationWorkflowProjectUsageSheet';

const hoisted = vi.hoisted(() => ({
    mockOnClose: vi.fn(),
    mockUseConnectedUserCatalogWorkflowCopiesQuery: vi.fn(),
    mockUseConnectedUserCodeWorkflowReferencesQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useConnectedUserCatalogWorkflowCopiesQuery: hoisted.mockUseConnectedUserCatalogWorkflowCopiesQuery,
        useConnectedUserCodeWorkflowReferencesQuery: hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery,
    };
});

vi.mock('@/components/ui/tooltip', () => ({
    Tooltip: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    TooltipContent: ({children}: {children: React.ReactNode}) => <div data-testid="tooltip-content">{children}</div>,
    TooltipTrigger: ({children}: {children: React.ReactNode}) => <div data-testid="tooltip-trigger">{children}</div>,
}));

const workflowTemplates = [{label: 'My Workflow', workflowUuid: 'uuid-1'}];

const renderSheet = (codeWorkflowProject = false) =>
    render(
        <AutomationWorkflowProjectUsageSheet
            codeWorkflowProject={codeWorkflowProject}
            onClose={hoisted.mockOnClose}
            projectName="My Project"
            workflowTemplates={workflowTemplates}
        />
    );

beforeEach(() => {
    hoisted.mockUseConnectedUserCatalogWorkflowCopiesQuery.mockReturnValue({
        data: {connectedUserCatalogWorkflowCopies: []},
        error: null,
        isLoading: false,
    });
});

afterEach(() => {
    resetAll();
});

describe('AutomationWorkflowProjectUsageSheet', () => {
    it('titles the sheet with the usage of the project', () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: {connectedUserCodeWorkflowReferences: []},
            error: null,
            isLoading: false,
        });

        renderSheet();

        expect(screen.getByText('Usage — My Project')).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'References'})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Copies'})).toBeInTheDocument();
    });

    it('shows a loading indicator while the references query is pending', () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: undefined,
            error: null,
            isLoading: true,
        });

        renderSheet();

        expect(document.querySelector('.animate-pulse')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('shows an empty state when no connected user references the project', () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: {connectedUserCodeWorkflowReferences: []},
            error: null,
            isLoading: false,
        });

        renderSheet();

        expect(screen.getByText(/no connected user has referenced this project yet/i)).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('renders a dangling badge with the dangling reason for a dangling reference', () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: {
                connectedUserCodeWorkflowReferences: [
                    {
                        automationWorkflowUuid: 'uuid-1',
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

        renderSheet();

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
                        automationWorkflowUuid: 'uuid-1',
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

        renderSheet();

        expect(screen.getByText('ext-1')).toBeInTheDocument();
        expect(screen.getByText('Enabled')).toBeInTheDocument();
        expect(screen.queryAllByRole('button')).toHaveLength(1);
        expect(screen.getByRole('button', {name: /close/i})).toBeInTheDocument();
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        expect(screen.getByText(/this list is read-only/i)).toBeInTheDocument();
        expect(screen.queryByRole('columnheader', {name: 'Environment'})).not.toBeInTheDocument();
        expect(hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery).toHaveBeenCalledWith(
            expect.objectContaining({environmentId: expect.any(String)}),
            expect.anything()
        );
        expect(document.querySelector('[data-slot="sheet-content"]')).toBeInTheDocument();
    });

    it('lists the connected users who copied a workflow on the Copies tab', async () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: {connectedUserCodeWorkflowReferences: []},
            error: null,
            isLoading: false,
        });
        hoisted.mockUseConnectedUserCatalogWorkflowCopiesQuery.mockReturnValue({
            data: {
                connectedUserCatalogWorkflowCopies: [
                    {
                        connectedUserEmail: 'jane@example.com',
                        connectedUserName: 'Jane Doe',
                        copiedFromWorkflowUuid: 'uuid-1',
                        enabled: true,
                        externalUserId: 'ext-2',
                        workflowUuid: 'copy-uuid-1',
                    },
                ],
            },
            error: null,
            isLoading: false,
        });

        renderSheet();

        expect(screen.queryByText('ext-2')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Copies'}));

        expect(screen.getByRole('tab', {name: 'Copies'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByText('My Workflow')).toBeInTheDocument();
        expect(screen.getByText('ext-2')).toBeInTheDocument();
        expect(screen.getByText('Jane Doe')).toBeInTheDocument();
        expect(screen.getByText('Enabled')).toBeInTheDocument();
        expect(screen.getByRole('columnheader', {name: 'Workflow'})).toBeInTheDocument();
        expect(screen.getByRole('columnheader', {name: 'External User'})).toBeInTheDocument();
        expect(screen.getByRole('columnheader', {name: 'Status'})).toBeInTheDocument();
        expect(hoisted.mockUseConnectedUserCatalogWorkflowCopiesQuery).toHaveBeenCalledWith(
            expect.objectContaining({automationWorkflowUuids: ['uuid-1'], environmentId: expect.any(String)}),
            expect.objectContaining({enabled: true})
        );
    });

    it('shows an empty state on the Copies tab when no connected user copied a workflow', async () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: {connectedUserCodeWorkflowReferences: []},
            error: null,
            isLoading: false,
        });

        renderSheet();

        await userEvent.click(screen.getByRole('tab', {name: 'Copies'}));

        expect(screen.getByText(/no connected user has copied a workflow of this project yet/i)).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });

    it('explains on the Copies tab of a code project that code workflows cannot be copied', async () => {
        hoisted.mockUseConnectedUserCodeWorkflowReferencesQuery.mockReturnValue({
            data: {connectedUserCodeWorkflowReferences: []},
            error: null,
            isLoading: false,
        });

        renderSheet(true);

        await userEvent.click(screen.getByRole('tab', {name: 'Copies'}));

        expect(screen.getByText("Code workflows can't be copied.")).toBeInTheDocument();
        expect(hoisted.mockUseConnectedUserCatalogWorkflowCopiesQuery).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({enabled: false})
        );
    });
});
