import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import type {UseQueryResult} from '@tanstack/react-query';

const mockQuerySuccess = <T,>(data: T, isLoading = false): UseQueryResult<T, Error> =>
    ({
        data,
        error: null,
        isError: false,
        isLoading,
        isPending: isLoading,
        isSuccess: !isLoading,
        refetch: vi.fn(),
        status: isLoading ? 'pending' : 'success',
    }) as unknown as UseQueryResult<T, Error>;

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: vi.fn(),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectWorkflowsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentsQuery: vi.fn(),
}));

vi.mock('use-debounce', () => ({
    useDebouncedCallback: (fn: (value: string) => void) => fn,
}));

const {useGetWorkspaceProjectsQuery} = await import('@/shared/queries/automation/projects.queries');
const {useGetProjectWorkflowsQuery} = await import('@/shared/queries/automation/projectWorkflows.queries');

const {useAiAgentsQuery} = await import('@/shared/middleware/graphql');

const mockUseAiAgentsQuery = vi.mocked(useAiAgentsQuery);
const mockUseGetWorkspaceProjectsQuery = vi.mocked(useGetWorkspaceProjectsQuery);
const mockUseGetProjectWorkflowsQuery = vi.mocked(useGetProjectWorkflowsQuery);

const AI_AGENTS = [
    {id: '42', title: 'Support Agent'},
    {id: '43', title: 'Sales Agent'},
];

const PROJECTS = [
    {id: 10, name: 'Alpha Project', projectWorkflowIds: [101], workspaceId: 1},
    {id: 20, name: 'Beta Project', projectWorkflowIds: [201, 202], workspaceId: 1},
    {id: 30, name: 'Empty Project', projectWorkflowIds: [], workspaceId: 1},
];

const WORKFLOWS_BY_PROJECT: Record<number, Array<{id: string; label: string; projectWorkflowId: number}>> = {
    10: [{id: 'wf-alpha-onboarding', label: 'Alpha Onboarding', projectWorkflowId: 101}],
    20: [
        {id: 'wf-beta-checkout', label: 'Beta Checkout', projectWorkflowId: 201},
        {id: 'wf-beta-refund', label: 'Beta Refund', projectWorkflowId: 202},
    ],
};

const setupMocks = () => {
    mockUseAiAgentsQuery.mockReturnValue(mockQuerySuccess({aiAgents: AI_AGENTS}) as never);
    mockUseGetWorkspaceProjectsQuery.mockReturnValue(mockQuerySuccess(PROJECTS) as never);
    mockUseGetProjectWorkflowsQuery.mockImplementation(
        (projectId: number) => mockQuerySuccess(WORKFLOWS_BY_PROJECT[projectId]) as never
    );
};

const renderMenu = async (extraProps?: Record<string, unknown>) => {
    const {default: ResourcePickerMenu} = await import('../ResourcePickerMenu');
    const onSelect = vi.fn();

    render(
        <ResourcePickerMenu
            onSelect={onSelect}
            trigger={<button>Open picker</button>}
            workspaceId={1}
            {...extraProps}
        />
    );

    return {onSelect};
};

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: /open picker/i}));
};

const drillIntoProject = async (projectName: string) => {
    await userEvent.click(await screen.findByText('Workflows'));
    await userEvent.click(await screen.findByText(projectName));
};

describe('ResourcePickerMenu', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        setupMocks();
    });

    it('shows the Workflows and AI Agents entries in the root menu', async () => {
        await renderMenu();
        await openMenu();

        await waitFor(() => {
            expect(screen.getByText('Workflows')).toBeInTheDocument();
        });

        expect(screen.getByText('AI Agents')).toBeInTheDocument();

        expect(screen.queryByText('Files')).not.toBeInTheDocument();
        expect(screen.queryByText('Data Tables')).not.toBeInTheDocument();
    });

    it('does not fetch projects while the picker is closed', async () => {
        await renderMenu();

        expect(mockUseGetWorkspaceProjectsQuery).toHaveBeenLastCalledWith({id: 1}, false);
    });

    it('does not fetch AI Agents while the picker is closed, and fetches them once open', async () => {
        await renderMenu();

        expect(mockUseAiAgentsQuery).toHaveBeenLastCalledWith({workspaceId: '1'}, {enabled: false});

        await openMenu();

        expect(mockUseAiAgentsQuery).toHaveBeenLastCalledWith({workspaceId: '1'}, {enabled: true});
    });

    describe('AI Agents', () => {
        it('fires onSelect with kind=aiAgent after picking an agent from the AI Agents list', async () => {
            const {onSelect} = await renderMenu();

            await openMenu();

            await userEvent.click(screen.getByText('AI Agents'));
            await userEvent.click(await screen.findByText('Sales Agent'));

            expect(onSelect).toHaveBeenCalledWith({id: '43', kind: 'aiAgent', name: 'Sales Agent'});
        });

        it('lists matching agents when searching from the root menu', async () => {
            await renderMenu();
            await openMenu();

            await userEvent.type(screen.getByPlaceholderText('Search resources…'), 'support');

            await waitFor(() => {
                expect(screen.getByText('Support Agent')).toBeInTheDocument();
            });

            expect(screen.queryByText('Sales Agent')).not.toBeInTheDocument();
        });
    });

    it('fetches the projects of the given workspace once open', async () => {
        await renderMenu();
        await openMenu();

        expect(mockUseGetWorkspaceProjectsQuery).toHaveBeenLastCalledWith({id: 1}, true);
    });

    it('does not fetch projects for an unknown workspace', async () => {
        await renderMenu({open: true, workspaceId: 0});

        expect(mockUseGetWorkspaceProjectsQuery).toHaveBeenLastCalledWith({id: 0}, false);
    });

    describe('workflow → project drill-down', () => {
        it('lists the projects that have workflows after clicking Workflows', async () => {
            await renderMenu();
            await openMenu();

            await userEvent.click(screen.getByText('Workflows'));

            await waitFor(() => {
                expect(screen.getByText('Workflows (pick a project)')).toBeInTheDocument();
                expect(screen.getByText('Alpha Project')).toBeInTheDocument();
                expect(screen.getByText('Beta Project')).toBeInTheDocument();
            });

            expect(screen.queryByText('Empty Project')).not.toBeInTheDocument();
        });

        it("shows only the selected project's workflows", async () => {
            await renderMenu();
            await openMenu();

            await drillIntoProject('Beta Project');

            await waitFor(() => {
                expect(screen.getByText('Beta Checkout')).toBeInTheDocument();
                expect(screen.getByText('Beta Refund')).toBeInTheDocument();
            });

            expect(screen.queryByText('Alpha Onboarding')).not.toBeInTheDocument();
            expect(mockUseGetProjectWorkflowsQuery).toHaveBeenLastCalledWith(20, true);
        });

        it('fires onSelect with kind=workflow and projectId/projectWorkflowId after picking a workflow', async () => {
            const {onSelect} = await renderMenu();

            await openMenu();
            await drillIntoProject('Alpha Project');

            await userEvent.click(await screen.findByText('Alpha Onboarding'));

            expect(onSelect).toHaveBeenCalledTimes(1);
            expect(onSelect).toHaveBeenCalledWith({
                id: 'wf-alpha-onboarding',
                kind: 'workflow',
                name: 'Alpha Onboarding',
                projectId: '10',
                projectWorkflowId: 101,
            });

            await waitFor(() => {
                expect(screen.queryByText('Alpha Onboarding')).not.toBeInTheDocument();
            });
        });

        it('steps back from a project to the project list', async () => {
            await renderMenu();
            await openMenu();

            await drillIntoProject('Alpha Project');

            await userEvent.click(await screen.findByText('Back'));

            await waitFor(() => {
                expect(screen.getByText('Workflows (pick a project)')).toBeInTheDocument();
            });
        });

        it('shows an empty message for a project without workflows', async () => {
            mockUseGetProjectWorkflowsQuery.mockImplementation(() => mockQuerySuccess([]) as never);

            await renderMenu();
            await openMenu();

            await drillIntoProject('Alpha Project');

            expect(await screen.findByText('No workflows.')).toBeInTheDocument();
        });
    });

    describe('search', () => {
        it('filters projects by name from the root menu', async () => {
            await renderMenu();
            await openMenu();

            await userEvent.type(screen.getByPlaceholderText('Search resources…'), 'beta');

            await waitFor(() => {
                expect(screen.getByText('Beta Project')).toBeInTheDocument();
            });

            expect(screen.queryByText('Alpha Project')).not.toBeInTheDocument();
        });

        it("filters the selected project's workflows by name", async () => {
            await renderMenu();
            await openMenu();

            await drillIntoProject('Beta Project');

            await userEvent.type(screen.getByPlaceholderText('Search resources…'), 'refund');

            await waitFor(() => {
                expect(screen.getByText('Beta Refund')).toBeInTheDocument();
            });

            expect(screen.queryByText('Beta Checkout')).not.toBeInTheDocument();
        });
    });

    describe('controlled open state', () => {
        it('renders the menu body when open is true without the trigger being clicked', async () => {
            await renderMenu({open: true});

            await waitFor(() => {
                expect(screen.getByText('Workflows')).toBeInTheDocument();
            });
        });

        it('reports the close back through onOpenChange when a workflow is picked', async () => {
            const onOpenChange = vi.fn();

            await renderMenu({onOpenChange, open: true});

            await drillIntoProject('Alpha Project');

            await userEvent.click(await screen.findByText('Alpha Onboarding'));

            await waitFor(() => {
                expect(onOpenChange).toHaveBeenCalledWith(false);
            });
        });
    });

    describe('Show-more pagination', () => {
        it('caps the workflow list and reveals more on demand', async () => {
            const manyWorkflows = Array.from({length: 25}, (_, index) => ({
                id: `wf-${index}`,
                label: `Workflow ${String(index).padStart(2, '0')}`,
                projectWorkflowId: 1000 + index,
            }));

            mockUseGetProjectWorkflowsQuery.mockImplementation(() => mockQuerySuccess(manyWorkflows) as never);

            await renderMenu();
            await openMenu();

            await drillIntoProject('Alpha Project');

            expect(await screen.findByText('Workflow 19')).toBeInTheDocument();
            expect(screen.queryByText('Workflow 20')).not.toBeInTheDocument();

            await userEvent.click(screen.getByText('Show 5 more…'));

            expect(await screen.findByText('Workflow 24')).toBeInTheDocument();
        });
    });
});
