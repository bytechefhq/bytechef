import {CommandItem} from '@/components/ui/command';
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

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentsQuery: vi.fn(),
    useGetAssetFilesQuery: vi.fn(),
    useWorkspaceProjectWorkflowsQuery: vi.fn(),
}));

vi.mock('use-debounce', () => ({
    useDebouncedCallback: (fn: (value: string) => void) => fn,
}));

const {useAiAgentsQuery, useGetAssetFilesQuery, useWorkspaceProjectWorkflowsQuery} =
    await import('@/shared/middleware/graphql');

const mockUseAiAgentsQuery = vi.mocked(useAiAgentsQuery);
const mockUseGetAssetFilesQuery = vi.mocked(useGetAssetFilesQuery);
const mockUseWorkspaceProjectWorkflowsQuery = vi.mocked(useWorkspaceProjectWorkflowsQuery);

const AI_AGENTS = [
    {id: '42', title: 'Support Agent'},
    {id: '43', title: 'Sales Agent'},
];

const ASSET_FILES = [
    {id: '7', name: 'notes.md'},
    {id: '8', name: 'report.pdf'},
];

const workspaceProjectWorkflow = (
    projectId: string,
    projectName: string,
    projectWorkflowId: string,
    workflowId: string,
    workflowLabel: string
) => ({projectId, projectName, projectWorkflowId, workflowId, workflowLabel});

const WORKSPACE_PROJECT_WORKFLOWS = [
    workspaceProjectWorkflow('10', 'Alpha Project', '101', 'wf-alpha-onboarding', 'Alpha Onboarding'),
    workspaceProjectWorkflow('20', 'Beta Project', '201', 'wf-beta-checkout', 'Beta Checkout'),
    workspaceProjectWorkflow('20', 'Beta Project', '202', 'wf-beta-refund', 'Beta Refund'),
];

const mockWorkspaceProjectWorkflows = (workspaceProjectWorkflows: ReturnType<typeof workspaceProjectWorkflow>[]) =>
    mockUseWorkspaceProjectWorkflowsQuery.mockReturnValue(mockQuerySuccess({workspaceProjectWorkflows}) as never);

const setupMocks = () => {
    mockUseAiAgentsQuery.mockReturnValue(mockQuerySuccess({aiAgents: AI_AGENTS}) as never);
    mockUseGetAssetFilesQuery.mockReturnValue(mockQuerySuccess({assetFiles: ASSET_FILES}) as never);
    mockWorkspaceProjectWorkflows(WORKSPACE_PROJECT_WORKFLOWS);
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

    it('shows the Workflows, AI Agents and Files entries in the root menu', async () => {
        await renderMenu();
        await openMenu();

        await waitFor(() => {
            expect(screen.getByText('Workflows')).toBeInTheDocument();
        });

        expect(screen.getByText('AI Agents')).toBeInTheDocument();
        expect(screen.getByText('Files')).toBeInTheDocument();

        expect(screen.queryByText('Data Tables')).not.toBeInTheDocument();
    });

    it('does not fetch the workspace workflows while the picker is closed', async () => {
        await renderMenu();

        expect(mockUseWorkspaceProjectWorkflowsQuery).toHaveBeenLastCalledWith({workspaceId: '1'}, {enabled: false});
    });

    it('does not fetch AI Agents while the picker is closed, and fetches them once open', async () => {
        await renderMenu();

        expect(mockUseAiAgentsQuery).toHaveBeenLastCalledWith({workspaceId: '1'}, {enabled: false});

        await openMenu();

        expect(mockUseAiAgentsQuery).toHaveBeenLastCalledWith({workspaceId: '1'}, {enabled: true});
    });

    describe('Files', () => {
        it('fetches files only once the picker is open', async () => {
            await renderMenu();

            expect(mockUseGetAssetFilesQuery).toHaveBeenLastCalledWith(expect.objectContaining({workspaceId: '1'}), {
                enabled: false,
            });

            await openMenu();

            expect(mockUseGetAssetFilesQuery).toHaveBeenLastCalledWith(expect.objectContaining({workspaceId: '1'}), {
                enabled: true,
            });
        });

        it('fires onSelect with kind=file after picking a file from the Files list', async () => {
            const {onSelect} = await renderMenu();

            await openMenu();

            await userEvent.click(screen.getByText('Files'));
            await userEvent.click(await screen.findByText('report.pdf'));

            expect(onSelect).toHaveBeenCalledWith({id: '8', kind: 'file', name: 'report.pdf'});
        });

        it('lists matching files when searching from the root menu', async () => {
            await renderMenu();
            await openMenu();

            await userEvent.type(screen.getByPlaceholderText('Search resources…'), 'notes');

            await waitFor(() => {
                expect(screen.getByText('notes.md')).toBeInTheDocument();
            });

            expect(screen.queryByText('report.pdf')).not.toBeInTheDocument();
        });
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

    it('fetches the workflows of the given workspace in one query once open', async () => {
        await renderMenu();
        await openMenu();

        expect(mockUseWorkspaceProjectWorkflowsQuery).toHaveBeenLastCalledWith({workspaceId: '1'}, {enabled: true});
    });

    it('does not fetch the workspace workflows for an unknown workspace', async () => {
        await renderMenu({open: true, workspaceId: 0});

        expect(mockUseWorkspaceProjectWorkflowsQuery).toHaveBeenLastCalledWith({workspaceId: '0'}, {enabled: false});
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
            expect(mockUseWorkspaceProjectWorkflowsQuery).toHaveBeenLastCalledWith({workspaceId: '1'}, {enabled: true});
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

        it("shows an empty message when the search matches none of the project's workflows", async () => {
            await renderMenu();
            await openMenu();

            await drillIntoProject('Alpha Project');

            await userEvent.type(screen.getByPlaceholderText('Search resources…'), 'missing');

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

    describe('custom branches', () => {
        it('drills into a caller-supplied branch and back to the root menu', async () => {
            const {CommandItem} = await import('@/components/ui/command');

            await renderMenu({
                customBranches: [
                    {
                        key: 'skills',
                        renderBranch: (onBack: () => void) => (
                            <CommandItem onSelect={onBack} value="skills-back">
                                Skills branch body
                            </CommandItem>
                        ),
                        renderRootItem: (onEnter: () => void) => (
                            <CommandItem onSelect={onEnter} value="root-skills">
                                Skills
                            </CommandItem>
                        ),
                    },
                ],
            });
            await openMenu();

            await userEvent.click(await screen.findByText('Skills'));

            expect(await screen.findByText('Skills branch body')).toBeInTheDocument();
            expect(screen.queryByText('Workflows')).not.toBeInTheDocument();

            await userEvent.click(screen.getByText('Skills branch body'));

            expect(await screen.findByText('Workflows')).toBeInTheDocument();
        });
    });

    describe('Show-more pagination', () => {
        it('caps the workflow list and reveals more on demand', async () => {
            mockWorkspaceProjectWorkflows(
                Array.from({length: 25}, (_, index) =>
                    workspaceProjectWorkflow(
                        '10',
                        'Alpha Project',
                        String(1000 + index),
                        `wf-${index}`,
                        `Workflow ${String(index).padStart(2, '0')}`
                    )
                )
            );

            await renderMenu();
            await openMenu();

            await drillIntoProject('Alpha Project');

            expect(await screen.findByText('Workflow 19')).toBeInTheDocument();
            expect(screen.queryByText('Workflow 20')).not.toBeInTheDocument();

            await userEvent.click(screen.getByText('Show 5 more…'));

            expect(await screen.findByText('Workflow 24')).toBeInTheDocument();
        });
    });

    describe('custom branches', () => {
        const connectorsBranch = (onCloseSpy: () => void) => ({
            key: 'connectors',
            renderBranch: (onBack: () => void, onClose: () => void) => (
                <>
                    <CommandItem onSelect={onBack} value="branch-back">
                        Branch back
                    </CommandItem>

                    <CommandItem
                        onSelect={() => {
                            onCloseSpy();
                            onClose();
                        }}
                        value="branch-close"
                    >
                        Manage connectors
                    </CommandItem>
                </>
            ),
            renderRootItem: (onEnter: () => void) => (
                <CommandItem onSelect={onEnter} value="root-connectors">
                    Connectors
                </CommandItem>
            ),
        });

        it('lists a custom branch at the root and drills into its body', async () => {
            await renderMenu({customBranches: [connectorsBranch(vi.fn())]});
            await openMenu();

            await userEvent.click(await screen.findByText('Connectors'));

            expect(await screen.findByText('Manage connectors')).toBeInTheDocument();
            expect(screen.queryByText('Workflows')).not.toBeInTheDocument();
        });

        it('returns to the root from the branch body', async () => {
            await renderMenu({customBranches: [connectorsBranch(vi.fn())]});
            await openMenu();

            await userEvent.click(await screen.findByText('Connectors'));
            await userEvent.click(await screen.findByText('Branch back'));

            expect(await screen.findByText('Workflows')).toBeInTheDocument();
        });

        it('closes the picker from the branch body', async () => {
            const onCloseSpy = vi.fn();
            const onOpenChange = vi.fn();

            await renderMenu({customBranches: [connectorsBranch(onCloseSpy)], onOpenChange});
            await openMenu();

            await userEvent.click(await screen.findByText('Connectors'));
            await userEvent.click(await screen.findByText('Manage connectors'));

            expect(onCloseSpy).toHaveBeenCalledTimes(1);
            expect(onOpenChange).toHaveBeenLastCalledWith(false);
        });
    });
});
