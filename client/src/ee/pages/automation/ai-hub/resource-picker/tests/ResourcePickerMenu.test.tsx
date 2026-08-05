import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import type {UseQueryResult} from '@tanstack/react-query';

/**
 * Typed mock factory — mirrors the shape used in AiHubComposer.test.tsx so
 * downstream type drift breaks here at compile time rather than silently.
 */
const mockQuerySuccess = <T,>(data: T): UseQueryResult<T, Error> =>
    ({
        data,
        dataUpdatedAt: 0,
        error: null,
        errorUpdateCount: 0,
        errorUpdatedAt: 0,
        failureCount: 0,
        failureReason: null,
        fetchStatus: 'idle',
        isError: false,
        isFetched: true,
        isFetchedAfterMount: true,
        isFetching: false,
        isInitialLoading: false,
        isLoading: false,
        isLoadingError: false,
        isPaused: false,
        isPending: false,
        isPlaceholderData: false,
        isRefetchError: false,
        isRefetching: false,
        isStale: false,
        isSuccess: true,
        promise: Promise.resolve(data),
        refetch: vi.fn(),
        status: 'success',
    }) as unknown as UseQueryResult<T, Error>;

// ── Mocks ──────────────────────────────────────────────────────────────────────

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useAiHubTasksQuery: vi.fn(),
        useDataTablesQuery: vi.fn(),
        useGetAssetFilesQuery: vi.fn(),
        useKnowledgeBasesQuery: vi.fn(),
        useWorkspaceMcpServersQuery: vi.fn(),
    };
});

vi.mock('@/ee/shared/mutations/automation/apiCollections.queries', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/ee/shared/mutations/automation/apiCollections.queries')>();

    return {
        ...actual,
        useGetApiCollectionsQuery: vi.fn(),
    };
});

vi.mock('@/shared/queries/automation/workflowExecutions.queries', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/queries/automation/workflowExecutions.queries')>();

    return {
        ...actual,
        useInfiniteWorkspaceProjectWorkflowExecutionsQuery: vi.fn(),
    };
});

vi.mock('@/shared/queries/automation/projects.queries', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/queries/automation/projects.queries')>();

    return {
        ...actual,
        useGetWorkspaceProjectsQuery: vi.fn(),
    };
});

vi.mock('@tanstack/react-query', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@tanstack/react-query')>();

    return {
        ...actual,
        useQueries: vi.fn(),
    };
});

// Bypass the 300 ms debounce so search tests don't need fake timers.
vi.mock('use-debounce', () => ({
    useDebouncedCallback: (fn: (value: string) => void) => fn,
}));

// ── Resolve mocked modules ────────────────────────────────────────────────────

const {
    useAiHubTasksQuery,
    useDataTablesQuery,
    useGetAssetFilesQuery,
    useKnowledgeBasesQuery,
    useWorkspaceMcpServersQuery,
} = await import('@/shared/middleware/graphql');

const {useGetApiCollectionsQuery} = await import('@/ee/shared/mutations/automation/apiCollections.queries');

const {useInfiniteWorkspaceProjectWorkflowExecutionsQuery} =
    await import('@/shared/queries/automation/workflowExecutions.queries');

const {useGetWorkspaceProjectsQuery} = await import('@/shared/queries/automation/projects.queries');

const {useQueries} = await import('@tanstack/react-query');

const mockUseAiHubTasksQuery = vi.mocked(useAiHubTasksQuery);
const mockUseDataTablesQuery = vi.mocked(useDataTablesQuery);
const mockUseGetAssetFilesQuery = vi.mocked(useGetAssetFilesQuery);
const mockUseKnowledgeBasesQuery = vi.mocked(useKnowledgeBasesQuery);
const mockUseWorkspaceMcpServersQuery = vi.mocked(useWorkspaceMcpServersQuery);
const mockUseGetApiCollectionsQuery = vi.mocked(useGetApiCollectionsQuery);
const mockUseInfiniteWorkspaceProjectWorkflowExecutionsQuery = vi.mocked(
    useInfiniteWorkspaceProjectWorkflowExecutionsQuery
);
const mockUseGetWorkspaceProjectsQuery = vi.mocked(useGetWorkspaceProjectsQuery);
const mockUseQueries = vi.mocked(useQueries);

// ── Fixtures ──────────────────────────────────────────────────────────────────

const FILES_FIXTURE = [
    {id: 'file-1', name: 'annual-report.pdf'},
    {id: 'file-2', name: 'brand-guide.png'},
];

// ── Setup helper ──────────────────────────────────────────────────────────────

const setupMocks = () => {
    mockUseGetAssetFilesQuery.mockReturnValue(mockQuerySuccess({assetFiles: FILES_FIXTURE}));
    mockUseDataTablesQuery.mockReturnValue(mockQuerySuccess({dataTables: []}));
    mockUseKnowledgeBasesQuery.mockReturnValue(mockQuerySuccess({knowledgeBases: []}));
    mockUseInfiniteWorkspaceProjectWorkflowExecutionsQuery.mockReturnValue({
        data: {pages: [{content: []}]},
        fetchNextPage: vi.fn(),
        hasNextPage: false,
        isFetchingNextPage: false,
    } as never);
    mockUseWorkspaceMcpServersQuery.mockReturnValue(mockQuerySuccess({workspaceMcpServers: []}));
    mockUseGetApiCollectionsQuery.mockReturnValue(mockQuerySuccess([]));
    mockUseAiHubTasksQuery.mockReturnValue(mockQuerySuccess({aiHubTasks: []}));
    mockUseGetWorkspaceProjectsQuery.mockReturnValue(mockQuerySuccess([]));
    mockUseQueries.mockReturnValue([]);
};

const renderMenu = async (extraProps?: Record<string, unknown>) => {
    const {default: ResourcePickerMenu} = await import('../ResourcePickerMenu');
    const onSelect = vi.fn();

    render(
        <ResourcePickerMenu
            environmentId={0}
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

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('ResourcePickerMenu', () => {
    beforeEach(() => {
        setupMocks();
    });

    it('shows the 8 reference-kind entries in the root menu', async () => {
        await renderMenu();
        await openMenu();

        await waitFor(() => {
            expect(screen.getByText('Workflows')).toBeInTheDocument();
            expect(screen.getByText('Files')).toBeInTheDocument();
            expect(screen.getByText('Data Tables')).toBeInTheDocument();
            expect(screen.getByText('Knowledge Bases')).toBeInTheDocument();
            expect(screen.getByText('Workflow Executions')).toBeInTheDocument();
            expect(screen.getByText('MCP Servers')).toBeInTheDocument();
            expect(screen.getByText('API Collections')).toBeInTheDocument();
            expect(screen.getByText('Previous Tasks')).toBeInTheDocument();
        });
    });

    it('hides the Tools root item when toolsBranch is not supplied', async () => {
        await renderMenu();
        await openMenu();

        await waitFor(() => {
            expect(screen.queryByText('Tools')).not.toBeInTheDocument();
        });
    });

    it('shows the Tools root item when toolsBranch is supplied', async () => {
        const {default: ResourcePickerMenu} = await import('../ResourcePickerMenu');
        const onSelect = vi.fn();

        const toolsBranch = {
            renderBranch: (onBack: () => void) => (
                <div>
                    <button onClick={onBack}>Back</button>

                    <span>Tools branch content</span>
                </div>
            ),
            renderRootItem: (onEnter: () => void) => <button onClick={onEnter}>Tools</button>,
        };

        render(
            <ResourcePickerMenu
                environmentId={0}
                onSelect={onSelect}
                toolsBranch={toolsBranch}
                trigger={<button>Open picker</button>}
                workspaceId={1}
            />
        );

        await openMenu();

        await waitFor(() => {
            expect(screen.getByText('Tools')).toBeInTheDocument();
        });
    });

    it('fires onSelect with the correct file payload and closes the menu after picking a file', async () => {
        const {onSelect} = await renderMenu();

        await openMenu();

        // Drill into the Files branch.
        await userEvent.click(screen.getByText('Files'));

        // Both files should now be visible in the Files sub-menu.
        await waitFor(() => {
            expect(screen.getByText('annual-report.pdf')).toBeInTheDocument();
        });

        // Pick the first file.
        await userEvent.click(screen.getByText('annual-report.pdf'));

        expect(onSelect).toHaveBeenCalledTimes(1);
        expect(onSelect).toHaveBeenCalledWith({
            id: 'file-1',
            kind: 'file',
            name: 'annual-report.pdf',
        });

        // The popover content should be gone after selection.
        await waitFor(() => {
            expect(screen.queryByText('annual-report.pdf')).not.toBeInTheDocument();
        });
    });

    it('filters files by the search query and hides non-matching results', async () => {
        await renderMenu();

        await openMenu();

        const searchInput = screen.getByPlaceholderText(/search resources/i);

        // Type a query that matches only the first file.
        await userEvent.type(searchInput, 'annual');

        await waitFor(() => {
            // Matching file should be visible.
            expect(screen.getByText('annual-report.pdf')).toBeInTheDocument();
            // Non-matching file should be absent.
            expect(screen.queryByText('brand-guide.png')).not.toBeInTheDocument();
        });
    });

    describe('workflow → project drill-down', () => {
        // Two projects, each with at least one workflow, to exercise multi-project selection.
        const PROJECT_ALPHA = {id: 10, name: 'Alpha Project'};
        const PROJECT_BETA = {id: 20, name: 'Beta Project'};

        // `id` is the workflow's UUID string; `projectWorkflowId` is the numeric join-entity id the
        // workflow editor actually needs. The two are distinct — exercising that distinction is the point
        // of these mocks (a regression where projectWorkflowId was derived from Number(id) produced NaN).
        const WORKFLOW_ALPHA_1 = {id: 'wf-alpha-onboarding', label: 'Alpha Onboarding', projectWorkflowId: 101};
        const WORKFLOW_BETA_1 = {id: 'wf-beta-checkout', label: 'Beta Checkout', projectWorkflowId: 201};
        const WORKFLOW_BETA_2 = {id: 'wf-beta-refund', label: 'Beta Refund', projectWorkflowId: 202};

        const setupWorkflowMocks = () => {
            mockUseGetWorkspaceProjectsQuery.mockReturnValue(
                mockQuerySuccess([PROJECT_ALPHA, PROJECT_BETA]) as ReturnType<typeof useGetWorkspaceProjectsQuery>
            );

            // useQueries returns one result per project in the same order as cappedProjects.
            mockUseQueries.mockReturnValue([
                {data: [WORKFLOW_ALPHA_1], status: 'success'},
                {data: [WORKFLOW_BETA_1, WORKFLOW_BETA_2], status: 'success'},
            ]);
        };

        it('shows the project list after clicking Workflows in the root menu', async () => {
            setupWorkflowMocks();
            await renderMenu();
            await openMenu();

            // Click the root "Workflows" item to drill into the projects level.
            await userEvent.click(screen.getByText('Workflows'));

            await waitFor(() => {
                // The project-picker heading should be visible.
                expect(screen.getByText('Workflows — pick a project')).toBeInTheDocument();
                // Both project names should appear as entries.
                expect(screen.getByText('Alpha Project')).toBeInTheDocument();
                expect(screen.getByText('Beta Project')).toBeInTheDocument();
            });
        });

        it("shows the selected project's workflows after clicking a project entry", async () => {
            setupWorkflowMocks();
            await renderMenu();
            await openMenu();

            await userEvent.click(screen.getByText('Workflows'));

            await waitFor(() => {
                expect(screen.getByText('Beta Project')).toBeInTheDocument();
            });

            // Drill into Beta Project — it has two workflows.
            await userEvent.click(screen.getByText('Beta Project'));

            await waitFor(() => {
                expect(screen.getByText('Beta Checkout')).toBeInTheDocument();
                expect(screen.getByText('Beta Refund')).toBeInTheDocument();
                // Alpha Project's workflow must NOT appear.
                expect(screen.queryByText('Alpha Onboarding')).not.toBeInTheDocument();
            });
        });

        it('fires onSelect with kind=workflow and projectId/projectWorkflowId after picking a workflow', async () => {
            setupWorkflowMocks();
            const {onSelect} = await renderMenu();
            await openMenu();

            // Drill: root → projects list.
            await userEvent.click(screen.getByText('Workflows'));

            await waitFor(() => {
                expect(screen.getByText('Alpha Project')).toBeInTheDocument();
            });

            // Drill: projects list → Alpha Project's workflows.
            await userEvent.click(screen.getByText('Alpha Project'));

            await waitFor(() => {
                expect(screen.getByText('Alpha Onboarding')).toBeInTheDocument();
            });

            // Pick the workflow.
            await userEvent.click(screen.getByText('Alpha Onboarding'));

            expect(onSelect).toHaveBeenCalledTimes(1);
            expect(onSelect).toHaveBeenCalledWith({
                id: 'wf-alpha-onboarding',
                kind: 'workflow',
                name: 'Alpha Onboarding',
                projectId: '10',
                projectWorkflowId: 101,
            });

            // Popover should close after selection.
            await waitFor(() => {
                expect(screen.queryByText('Alpha Onboarding')).not.toBeInTheDocument();
            });
        });
    });

    describe('Files branch — Show-more pagination', () => {
        // SECTION_INITIAL_CAP = 20, SECTION_EXPAND_INCREMENT = 50
        const INITIAL_CAP = 20;
        const EXTRA_FILES = 5;
        const TOTAL_FILES = INITIAL_CAP + EXTRA_FILES;

        const buildFileFixture = () =>
            Array.from({length: TOTAL_FILES}, (_, fileIndex) => ({
                id: `f-${fileIndex + 1}`,
                name: `file-${String(fileIndex + 1).padStart(3, '0')}.txt`,
            }));

        it('caps the list at SECTION_INITIAL_CAP and shows a "Show N more…" item', async () => {
            mockUseGetAssetFilesQuery.mockReturnValue(mockQuerySuccess({assetFiles: buildFileFixture()}));

            await renderMenu();
            await openMenu();

            // Drill into Files browse-mode branch.
            await userEvent.click(screen.getByText('Files'));

            await waitFor(() => {
                // First capped file should be visible.
                expect(screen.getByText('file-001.txt')).toBeInTheDocument();
                // Last capped file (index 20 = INITIAL_CAP) should be visible.
                expect(screen.getByText('file-020.txt')).toBeInTheDocument();
                // First file beyond the cap should NOT be visible yet.
                expect(screen.queryByText('file-021.txt')).not.toBeInTheDocument();
                // The "Show more" affordance must be present with the exact label.
                expect(screen.getByText(`Show ${EXTRA_FILES} more…`)).toBeInTheDocument();
            });
        });

        it('reveals the hidden files after clicking the "Show N more…" item', async () => {
            mockUseGetAssetFilesQuery.mockReturnValue(mockQuerySuccess({assetFiles: buildFileFixture()}));

            await renderMenu();
            await openMenu();

            await userEvent.click(screen.getByText('Files'));

            await waitFor(() => {
                expect(screen.getByText(`Show ${EXTRA_FILES} more…`)).toBeInTheDocument();
            });

            // Click the "Show more" item to expand.
            await userEvent.click(screen.getByText(`Show ${EXTRA_FILES} more…`));

            await waitFor(() => {
                // Previously hidden files must now appear.
                expect(screen.getByText('file-021.txt')).toBeInTheDocument();
                expect(screen.getByText(`file-${String(TOTAL_FILES).padStart(3, '0')}.txt`)).toBeInTheDocument();
                // The "Show more" affordance should be gone — all files are now visible.
                expect(screen.queryByText(`Show ${EXTRA_FILES} more…`)).not.toBeInTheDocument();
            });
        });
    });

    it('shows a matching MCP server in search-mode results', async () => {
        // Regression: search mode previously rendered only Files / Workflows / Data Tables /
        // Knowledge Bases, so a query matching only an MCP server left the dropdown blank even
        // though `hasResults` counted the MCP server and suppressed the empty state.
        mockUseWorkspaceMcpServersQuery.mockReturnValue(
            mockQuerySuccess({
                workspaceMcpServers: [{enabled: true, id: 'mcp-1', name: 'orbital-relay-mcp'}],
            })
        );

        await renderMenu();

        await openMenu();

        const searchInput = screen.getByPlaceholderText(/search resources/i);

        // Type a query that matches the MCP server name.
        await userEvent.type(searchInput, 'orbital');

        await waitFor(() => {
            // The MCP server must appear in the search results.
            expect(screen.getByText('orbital-relay-mcp')).toBeInTheDocument();
            // The empty state must not be shown.
            expect(screen.queryByText('No resources found.')).not.toBeInTheDocument();
        });
    });
});
