import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import EnvironmentPromotionDialog from './EnvironmentPromotionDialog';

beforeAll(() => {
    // Radix Select relies on pointer-capture APIs that jsdom does not implement.
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

// vi.mock factories hoist above module-scope `const` declarations, so any mock implementation
// that needs a stable reference across renders must come from vi.hoisted — see CLAUDE.md's
// "Vitest mock factory hoisting".
const {mockMutateAsync, onPromotedMock} = vi.hoisted(() => ({
    mockMutateAsync: vi.fn(),
    onPromotedMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useEnvironmentPromotionPreviewQuery: vi.fn(),
        useEnvironmentsQuery: vi.fn(),
        usePromoteToEnvironmentMutation: vi.fn(),
    };
});

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
    },
}));

const {
    useEnvironmentPromotionPreviewQuery: mockUseEnvironmentPromotionPreviewQuery,
    useEnvironmentsQuery: mockUseEnvironmentsQuery,
    usePromoteToEnvironmentMutation: mockUsePromoteToEnvironmentMutation,
} = await import('@/shared/middleware/graphql');
const {useGetWorkspaceConnectionsQuery: mockUseGetWorkspaceConnectionsQuery} =
    await import('@/shared/queries/automation/connections.queries');
const {toast: mockToast} = await import('sonner');
const {PromotionResourceType} = await import('@/shared/middleware/graphql');

const ENVIRONMENTS = [
    {id: '0', name: 'Development'},
    {id: '1', name: 'Staging'},
    {id: '2', name: 'Production'},
];

const PREVIEW_CONNECTION = {
    componentName: 'slack',
    connectionVersion: 1,
    sourceConnectionId: 'conn-1',
    sourceConnectionName: 'Slack Prod',
    suggestedTargetConnectionId: '9',
    usedBy: ['Send Message'],
};

const PREVIEW_PROJECT = {
    projectId: 'project-1',
    projectName: 'My Project',
    sourceProjectVersion: 1,
    targetProjectVersion: 3,
};

const makePreview = (overrides: Record<string, unknown> = {}) => ({
    connections: [PREVIEW_CONNECTION],
    existingTargetId: null,
    existingTargetName: null,
    projects: [PREVIEW_PROJECT],
    resourceType: PromotionResourceType.ApiCollection,
    sourceEnvironmentId: '0',
    sourceId: 'source-1',
    targetEnvironmentId: '1',
    warnings: [] as string[],
    ...overrides,
});

const TARGET_CONNECTIONS = [
    {environmentId: 1, id: 9, name: 'Slack Staging'},
    {environmentId: 1, id: 10, name: 'Slack Staging (2)'},
];

const defaultProps = {
    onClose: vi.fn(),
    onPromoted: onPromotedMock,
    resourceType: PromotionResourceType.ApiCollection,
    sourceEnvironmentId: 0,
    sourceId: 'source-1',
    sourceName: 'My API Collection',
    workspaceId: 100,
};

describe('EnvironmentPromotionDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        environmentStore.setState({currentEnvironmentId: 0});

        vi.mocked(mockUseEnvironmentsQuery).mockReturnValue({
            data: {environments: ENVIRONMENTS},
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockReturnValue({
            data: {environmentPromotionPreview: makePreview()},
            isLoading: false,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        vi.mocked(mockUsePromoteToEnvironmentMutation).mockReturnValue({
            isPending: false,
            mutateAsync: mockMutateAsync,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        vi.mocked(mockUseGetWorkspaceConnectionsQuery).mockReturnValue({
            data: TARGET_CONNECTIONS,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        mockMutateAsync.mockResolvedValue({
            promoteToEnvironment: {
                created: true,
                targetId: 'target-1',
                targetUrl: null,
                unresolvedConnectionIds: [],
            },
        });
    });

    it('defaults the target environment to the next one after the source (DEV -> STAGING)', async () => {
        render(<EnvironmentPromotionDialog {...defaultProps} />);

        // Source is DEVELOPMENT (0); the next environment is STAGING (1). The target select
        // renders environment labels from ENVIRONMENT_CONFIGS (uppercase), not the raw query name.
        await waitFor(() => expect(screen.getByText('STAGING')).toBeInTheDocument());
    });

    it('wraps the default target environment around to the first non-source one (PROD -> DEV)', async () => {
        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockReturnValue({
            data: {environmentPromotionPreview: makePreview({sourceEnvironmentId: '2', targetEnvironmentId: '0'})},
            isLoading: false,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        render(<EnvironmentPromotionDialog {...defaultProps} sourceEnvironmentId={2} />);

        await waitFor(() => expect(screen.getByText('DEVELOPMENT')).toBeInTheDocument());
    });

    it('renders the preview summary: create badge, project version and warnings', async () => {
        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockReturnValue({
            data: {environmentPromotionPreview: makePreview({warnings: ['contextPath differs from the target']})},
            isLoading: false,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        expect(await screen.findByText('Will create')).toBeInTheDocument();

        expect(screen.getByText(/v1 → v3/)).toBeInTheDocument();

        expect(screen.getByText('contextPath differs from the target')).toBeInTheDocument();
    });

    it('renders "Will update <name>" when a target already exists', async () => {
        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockReturnValue({
            data: {
                environmentPromotionPreview: makePreview({
                    existingTargetId: 'existing-1',
                    existingTargetName: 'Existing Collection',
                }),
            },
            isLoading: false,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        expect(await screen.findByText('Will update Existing Collection')).toBeInTheDocument();
    });

    it('pre-selects the suggested target connection for each connection row', async () => {
        render(<EnvironmentPromotionDialog {...defaultProps} />);

        expect(await screen.findByText('Slack Staging')).toBeInTheDocument();
    });

    it('warns that the target will be created disabled when a connection is left unresolved', async () => {
        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockReturnValue({
            data: {
                environmentPromotionPreview: makePreview({
                    connections: [{...PREVIEW_CONNECTION, suggestedTargetConnectionId: null}],
                }),
            },
            isLoading: false,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        expect(await screen.findByText(/connection\(s\) are unresolved/)).toBeInTheDocument();
    });

    it('sends only resolved connections when promoting, choosing "Unresolved" removes it from the payload', async () => {
        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await screen.findByText('Slack Staging');

        await user.click(screen.getByLabelText('Target connection for Slack Prod'));

        await user.click(screen.getByText('Unresolved'));

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));

        expect(mockMutateAsync).toHaveBeenCalledWith({
            input: {
                connectionMappings: [],
                resourceType: PromotionResourceType.ApiCollection,
                sourceId: 'source-1',
                targetEnvironmentId: '1',
            },
        });
    });

    it('sends the resolved connection mapping payload when promoting with the default suggestion', async () => {
        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await screen.findByText('Slack Staging');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));

        expect(mockMutateAsync).toHaveBeenCalledWith({
            input: {
                connectionMappings: [{sourceConnectionId: 'conn-1', targetConnectionId: '9'}],
                resourceType: PromotionResourceType.ApiCollection,
                sourceId: 'source-1',
                targetEnvironmentId: '1',
            },
        });
    });

    it('calls onPromoted and switches the environment store when the toast action is clicked', async () => {
        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await screen.findByText('Slack Staging');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() =>
            expect(onPromotedMock).toHaveBeenCalledWith({
                created: true,
                targetEnvironmentId: 1,
                targetId: 'target-1',
            })
        );

        expect(mockToast.success).toHaveBeenCalledTimes(1);

        const [, toastOptions] = vi.mocked(mockToast.success).mock.calls[0];

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const action = (toastOptions as any).action;

        expect(action.label).toBe('View in STAGING');

        // The store must not have switched yet — only the toast action switches it, never the
        // promotion itself.
        expect(environmentStore.getState().currentEnvironmentId).toBe(0);

        action.onClick();

        await waitFor(() => expect(environmentStore.getState().currentEnvironmentId).toBe(1));
    });

    it('shows the new URL in the success toast when promoting an MCP server creates a counterpart', async () => {
        const user = userEvent.setup();

        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockReturnValue({
            data: {environmentPromotionPreview: makePreview({resourceType: PromotionResourceType.McpServer})},
            isLoading: false,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        mockMutateAsync.mockResolvedValue({
            promoteToEnvironment: {
                created: true,
                targetId: 'target-1',
                targetUrl: 'https://example.com/api/automation/new-secret/mcp',
                unresolvedConnectionIds: [],
            },
        });

        render(<EnvironmentPromotionDialog {...defaultProps} resourceType={PromotionResourceType.McpServer} />);

        await screen.findByText('Slack Staging');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockToast.success).toHaveBeenCalledTimes(1));

        const [, toastOptions] = vi.mocked(mockToast.success).mock.calls[0];

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((toastOptions as any).description).toContain(
            'New URL: https://example.com/api/automation/new-secret/mcp'
        );
    });

    it('does not show the new URL in the success toast when re-promoting updates an existing MCP server counterpart', async () => {
        const user = userEvent.setup();

        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockReturnValue({
            data: {
                environmentPromotionPreview: makePreview({
                    existingTargetId: 'existing-mcp-1',
                    existingTargetName: 'Existing MCP Server',
                    resourceType: PromotionResourceType.McpServer,
                }),
            },
            isLoading: false,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        // The MCP/A2A handlers populate targetUrl on the update path too (the URL is unchanged,
        // never re-minted) — this pins that the toast still must not claim it is new.
        mockMutateAsync.mockResolvedValue({
            promoteToEnvironment: {
                created: false,
                targetId: 'existing-mcp-1',
                targetUrl: 'https://example.com/api/automation/unchanged-secret/mcp',
                unresolvedConnectionIds: [],
            },
        });

        render(<EnvironmentPromotionDialog {...defaultProps} resourceType={PromotionResourceType.McpServer} />);

        await screen.findByText('Slack Staging');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockToast.success).toHaveBeenCalledTimes(1));

        const [, toastOptions] = vi.mocked(mockToast.success).mock.calls[0];

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((toastOptions as any).description).not.toContain('New URL:');
    });
});
