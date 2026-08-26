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
        useAiAgentsQuery: vi.fn(),
        useAiHubChatsQuery: vi.fn(),
        useDataTablesQuery: vi.fn(),
        useGetAssetFilesQuery: vi.fn(),
        useKnowledgeBasesQuery: vi.fn(),
        useWorkspaceMcpServersQuery: vi.fn(),
        useWorkspaceProjectWorkflowsQuery: vi.fn(),
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

// Bypass the 300 ms debounce so search tests don't need fake timers.
vi.mock('use-debounce', () => ({
    useDebouncedCallback: (fn: (value: string) => void) => fn,
}));

// ── Resolve mocked modules ────────────────────────────────────────────────────

const {
    useAiAgentsQuery,
    useAiHubChatsQuery,
    useDataTablesQuery,
    useGetAssetFilesQuery,
    useKnowledgeBasesQuery,
    useWorkspaceMcpServersQuery,
    useWorkspaceProjectWorkflowsQuery,
} = await import('@/shared/middleware/graphql');

const {useGetApiCollectionsQuery} = await import('@/ee/shared/mutations/automation/apiCollections.queries');

const {useInfiniteWorkspaceProjectWorkflowExecutionsQuery} =
    await import('@/shared/queries/automation/workflowExecutions.queries');

const mockUseAiAgentsQuery = vi.mocked(useAiAgentsQuery);
const mockUseAiHubChatsQuery = vi.mocked(useAiHubChatsQuery);
const mockUseDataTablesQuery = vi.mocked(useDataTablesQuery);
const mockUseGetAssetFilesQuery = vi.mocked(useGetAssetFilesQuery);
const mockUseKnowledgeBasesQuery = vi.mocked(useKnowledgeBasesQuery);
const mockUseWorkspaceMcpServersQuery = vi.mocked(useWorkspaceMcpServersQuery);
const mockUseGetApiCollectionsQuery = vi.mocked(useGetApiCollectionsQuery);
const mockUseInfiniteWorkspaceProjectWorkflowExecutionsQuery = vi.mocked(
    useInfiniteWorkspaceProjectWorkflowExecutionsQuery
);
const mockUseWorkspaceProjectWorkflowsQuery = vi.mocked(useWorkspaceProjectWorkflowsQuery);

// ── Fixtures ──────────────────────────────────────────────────────────────────

const FILES_FIXTURE = [
    {id: 'file-1', name: 'annual-report.pdf'},
    {id: 'file-2', name: 'brand-guide.png'},
];

// `title` is the display name and `name` the internal handle — they differ here on purpose, so a renderer
// that reached for `name` would fail these tests rather than passing on a fixture where both read alike.
const AI_AGENTS_FIXTURE = [
    {id: 'agent-1', name: 'support_agent', title: 'Support Agent', unpublishedChanges: false},
    {id: 'agent-2', name: 'scheduled_two', title: 'Scheduled2', unpublishedChanges: true},
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
    mockUseAiHubChatsQuery.mockReturnValue(mockQuerySuccess({aiHubChats: []}));
    mockUseWorkspaceProjectWorkflowsQuery.mockReturnValue(mockQuerySuccess({workspaceProjectWorkflows: []}));
    mockUseAiAgentsQuery.mockReturnValue(mockQuerySuccess({aiAgents: AI_AGENTS_FIXTURE}));
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

    it('shows the 9 reference-kind entries in the root menu', async () => {
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
            expect(screen.getByText('AI Agents')).toBeInTheDocument();
            expect(screen.getByText('Previous Chats')).toBeInTheDocument();
        });
    });

    describe('AI Agents branch', () => {
        it("lists the workspace's agents by title after drilling into the branch", async () => {
            await renderMenu();
            await openMenu();

            await userEvent.click(await screen.findByText('AI Agents'));

            await waitFor(() => {
                expect(screen.getByText('Support Agent')).toBeInTheDocument();
                expect(screen.getByText('Scheduled2')).toBeInTheDocument();
            });

            // The internal handle must never be what the user reads.
            expect(screen.queryByText('support_agent')).not.toBeInTheDocument();
        });

        it('fires onSelect with kind=aiAgent and the agent id after picking one', async () => {
            const {onSelect} = await renderMenu();
            await openMenu();

            await userEvent.click(await screen.findByText('AI Agents'));
            await userEvent.click(await screen.findByText('Scheduled2'));

            await waitFor(() => {
                expect(onSelect).toHaveBeenCalledWith({id: 'agent-2', kind: 'aiAgent', name: 'Scheduled2'});
            });
        });

        it('shows a matching agent in search-mode results', async () => {
            await renderMenu();
            await openMenu();

            await userEvent.type(screen.getByPlaceholderText('Search resources…'), 'scheduled');

            await waitFor(() => {
                expect(screen.getByText('Scheduled2')).toBeInTheDocument();
                expect(screen.queryByText('Support Agent')).not.toBeInTheDocument();
            });
        });
    });

    // The picker is the composer's second trigger surface: '@' in the textarea sets a store flag that arrives
    // here as `open`. Without a controlled mode the keystroke has no way in, which is what left '@' inert.
    describe('controlled open state', () => {
        it('renders the menu body when open is true without the trigger being clicked', async () => {
            await renderMenu({open: true});

            await waitFor(() => {
                expect(screen.getByText('Files')).toBeInTheDocument();
            });
        });

        it('reports the close back through onOpenChange when a resource is picked', async () => {
            const onOpenChange = vi.fn();

            await renderMenu({onOpenChange, open: true});

            await userEvent.click(await screen.findByText('Files'));
            await userEvent.click(await screen.findByText('annual-report.pdf'));

            // Radix never mediates a close-by-selection, so without the explicit report a controlled
            // consumer's flag would stay true and the popover could never be reopened.
            await waitFor(() => {
                expect(onOpenChange).toHaveBeenCalledWith(false);
            });
        });
    });

    it('shows no custom branch root items when customBranches is not supplied', async () => {
        await renderMenu();
        await openMenu();

        await waitFor(() => {
            expect(screen.queryByText('Connectors')).not.toBeInTheDocument();
            expect(screen.queryByText('Skills')).not.toBeInTheDocument();
        });
    });

    // Each branch must reach ITS OWN body: the menu stores the branch key in the path and looks it up, so a
    // lookup that ignored the key (or a single shared slot) would silently render the first branch for both.
    it('renders every custom branch root item and drills into the matching branch body', async () => {
        const customBranches = [
            {
                key: 'connectors',
                renderBranch: (onBack: () => void) => (
                    <div>
                        <button onClick={onBack}>Back</button>

                        <span>Connectors branch content</span>
                    </div>
                ),
                renderRootItem: (onEnter: () => void) => <button onClick={onEnter}>Connectors</button>,
            },
            {
                key: 'skills',
                renderBranch: () => <span>Skills branch content</span>,
                renderRootItem: (onEnter: () => void) => <button onClick={onEnter}>Skills</button>,
            },
        ];

        await renderMenu({customBranches});
        await openMenu();

        await waitFor(() => {
            expect(screen.getByText('Connectors')).toBeInTheDocument();
            expect(screen.getByText('Skills')).toBeInTheDocument();
        });

        await userEvent.click(screen.getByRole('button', {name: 'Skills'}));

        await waitFor(() => {
            expect(screen.getByText('Skills branch content')).toBeInTheDocument();
        });

        expect(screen.queryByText('Connectors branch content')).not.toBeInTheDocument();
    });

    it('returns to the root menu from a custom branch via onBack', async () => {
        const customBranches = [
            {
                key: 'connectors',
                renderBranch: (onBack: () => void) => <button onClick={onBack}>Back</button>,
                renderRootItem: (onEnter: () => void) => <button onClick={onEnter}>Connectors</button>,
            },
        ];

        await renderMenu({customBranches});
        await openMenu();

        await userEvent.click(await screen.findByRole('button', {name: 'Connectors'}));
        await userEvent.click(await screen.findByRole('button', {name: 'Back'}));

        await waitFor(() => {
            expect(screen.getByText('Files')).toBeInTheDocument();
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
        // The server returns ONE flat, workspace-wide list; the two-level project → workflow drill-down is regrouped
        // client-side by `groupWorkflowsByProject`. Two projects, each with at least one workflow, to exercise
        // multi-project grouping from a single flat response.
        //
        // `workflowId` is the workflow's UUID string; `projectWorkflowId` is the join-entity id the workflow editor
        // actually needs, and it arrives as a GraphQL ID (a string). The two are distinct — exercising that
        // distinction is the point of these fixtures (a regression where projectWorkflowId was derived from
        // Number(workflowId) produced NaN).
        const WORKSPACE_PROJECT_WORKFLOWS = [
            {
                projectId: '10',
                projectName: 'Alpha Project',
                projectWorkflowId: '101',
                workflowId: 'wf-alpha-onboarding',
                workflowLabel: 'Alpha Onboarding',
            },
            {
                projectId: '20',
                projectName: 'Beta Project',
                projectWorkflowId: '201',
                workflowId: 'wf-beta-checkout',
                workflowLabel: 'Beta Checkout',
            },
            {
                projectId: '20',
                projectName: 'Beta Project',
                projectWorkflowId: '202',
                workflowId: 'wf-beta-refund',
                workflowLabel: 'Beta Refund',
            },
        ];

        const setupWorkflowMocks = () => {
            mockUseWorkspaceProjectWorkflowsQuery.mockReturnValue(
                mockQuerySuccess({workspaceProjectWorkflows: WORKSPACE_PROJECT_WORKFLOWS})
            );
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
