import {GraphQLResponseError} from '@/shared/middleware/graphqlFetcher';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {render, screen, waitFor, within} from '@/shared/util/test-utils';
import userEvent, {type UserEvent} from '@testing-library/user-event';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import EnvironmentPromotionDialog from '../EnvironmentPromotionDialog';

beforeAll(() => {
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const {mockMutateAsync, mockNavigate, mockRefetch, onPromotedMock} = vi.hoisted(() => ({
    mockMutateAsync: vi.fn(),
    mockNavigate: vi.fn(),
    mockRefetch: vi.fn(),
    onPromotedMock: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => mockNavigate,
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

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
        success: vi.fn(),
        warning: vi.fn(),
    },
}));

const {
    useEnvironmentPromotionPreviewQuery: mockUseEnvironmentPromotionPreviewQuery,
    useEnvironmentsQuery: mockUseEnvironmentsQuery,
    usePromoteToEnvironmentMutation: mockUsePromoteToEnvironmentMutation,
} = await import('@/shared/middleware/graphql');
const {toast: mockToast} = await import('sonner');
const {PromotionChangeKind, PromotionResourceType, PromotionSchemaChangeKind} =
    await import('@/shared/middleware/graphql');

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
    targetCandidates: [
        {connectionId: '9', name: 'Slack Staging'},
        {connectionId: '10', name: 'Slack Staging (2)'},
    ],
    usedBy: ['Send Message'],
};

const PREVIEW_PROJECT = {
    projectId: 'project-1',
    projectName: 'My Project',
    sourceProjectVersion: 1,
    targetProjectVersion: 3,
};

const DESTRUCTIVE_CHANGE = {
    assetType: 'MCP_TOOL',
    destructive: true,
    key: 'tool-extra',
    kind: PromotionChangeKind.Delete,
    name: 'Extra tool',
    parentName: null,
};

const makePreview = (overrides: Record<string, unknown> = {}) => ({
    changes: [] as unknown[],
    connections: [PREVIEW_CONNECTION],
    destructiveChangesHash: 'hash-1',
    existingTargetId: null,
    existingTargetName: null,
    projects: [PREVIEW_PROJECT],
    resourceType: PromotionResourceType.ApiCollection,
    schemaChanges: [] as unknown[],
    sourceEnvironmentId: '0',
    sourceId: 'source-1',
    targetEnvironmentId: '1',
    warnings: [] as string[],
    ...overrides,
});

const mockPreview = (preview: Record<string, unknown>) => {
    vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockReturnValue({
        data: {environmentPromotionPreview: preview},
        isLoading: false,
        refetch: mockRefetch,
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } as any);
};

const getCurrentStep = () =>
    within(screen.getByRole('navigation', {name: 'Steps'})).getByRole('button', {current: 'step'});

const advanceToStep = async (user: UserEvent, stepLabel: string) => {
    for (let attempt = 0; attempt < 4; attempt++) {
        if (getCurrentStep().textContent?.startsWith(stepLabel)) {
            return;
        }

        await user.click(screen.getByRole('button', {name: 'Next'}));
    }

    expect(getCurrentStep()).toHaveTextContent(stepLabel);
};

const defaultProps = {
    onClose: vi.fn(),
    onPromoted: onPromotedMock,
    resourceType: PromotionResourceType.ApiCollection,
    sourceEnvironmentId: 0,
    sourceId: 'source-1',
    sourceName: 'My API Collection',
    targetListPath: '/automation/api-platform/api-collections',
};

describe('EnvironmentPromotionDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        environmentStore.setState({currentEnvironmentId: 0});

        vi.mocked(mockUseEnvironmentsQuery).mockReturnValue({
            data: {environments: ENVIRONMENTS},
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        mockPreview(makePreview());

        vi.mocked(mockUsePromoteToEnvironmentMutation).mockReturnValue({
            isPending: false,
            mutateAsync: mockMutateAsync,
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

        await waitFor(() => expect(screen.getByText('STAGING')).toBeInTheDocument());
    });

    it('requests the preview with the source environment', async () => {
        render(<EnvironmentPromotionDialog {...defaultProps} sourceEnvironmentId={2} />);

        await waitFor(() =>
            expect(mockUseEnvironmentPromotionPreviewQuery).toHaveBeenCalledWith(
                expect.objectContaining({sourceEnvironmentId: '2', sourceId: 'source-1', targetEnvironmentId: 0}),
                expect.anything()
            )
        );
    });

    it('wraps the default target environment around to the first non-source one (PROD -> DEV)', async () => {
        mockPreview(makePreview({sourceEnvironmentId: '2', targetEnvironmentId: '0'}));

        render(<EnvironmentPromotionDialog {...defaultProps} sourceEnvironmentId={2} />);

        await waitFor(() => expect(screen.getByText('DEVELOPMENT')).toBeInTheDocument());
    });

    it('renders the preview summary: create badge, project version and warnings', async () => {
        mockPreview(makePreview({warnings: ['contextPath differs from the target']}));

        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        expect(await screen.findByText('Will create')).toBeInTheDocument();

        await advanceToStep(user, 'Review changes');

        expect(screen.getByText(/v1 → v3/)).toBeInTheDocument();

        expect(screen.getByText('contextPath differs from the target')).toBeInTheDocument();
    });

    it('renders "Will update <name>" when a target already exists', async () => {
        mockPreview(
            makePreview({
                existingTargetId: 'existing-1',
                existingTargetName: 'Existing Collection',
            })
        );

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        expect(await screen.findByText('Will update Existing Collection')).toBeInTheDocument();
    });

    it('pre-selects the suggested target connection for each connection row', async () => {
        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Connections');

        expect(await screen.findByText('Slack Staging')).toBeInTheDocument();
    });

    it('warns about unbound connections when a connection is left unresolved for a new target', async () => {
        mockPreview(
            makePreview({
                connections: [{...PREVIEW_CONNECTION, suggestedTargetConnectionId: null}],
            })
        );

        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Connections');

        expect(await screen.findByText(/connection\(s\) are unresolved/)).toBeInTheDocument();
    });

    it('does not offer "Unresolved" for a connection the server would bind to its suggestion anyway', async () => {
        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Connections');

        await user.click(screen.getByLabelText('Target connection for Slack Prod'));

        expect(await screen.findByRole('option', {name: 'Slack Staging (2)'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Unresolved'})).not.toBeInTheDocument();
    });

    it('sends no mapping when promoting with the default suggestion, so existing per-node bindings are kept', async () => {
        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));

        expect(mockMutateAsync).toHaveBeenCalledWith({
            input: {
                connectionMappings: [],
                expectedDestructiveChangesHash: 'hash-1',
                resourceType: PromotionResourceType.ApiCollection,
                sourceEnvironmentId: '0',
                sourceId: 'source-1',
                targetEnvironmentId: '1',
            },
        });
    });

    it('sends the mapping the user changed away from the suggestion', async () => {
        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Connections');

        await user.click(screen.getByLabelText('Target connection for Slack Prod'));

        await user.click(await screen.findByRole('option', {name: 'Slack Staging (2)'}));

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));

        expect(mockMutateAsync).toHaveBeenCalledWith({
            input: {
                connectionMappings: [{sourceConnectionId: 'conn-1', targetConnectionId: '10'}],
                expectedDestructiveChangesHash: 'hash-1',
                resourceType: PromotionResourceType.ApiCollection,
                sourceEnvironmentId: '0',
                sourceId: 'source-1',
                targetEnvironmentId: '1',
            },
        });
    });

    it('warns that unresolved connections leave nodes unbound when updating an existing target', async () => {
        mockPreview(
            makePreview({
                connections: [{...PREVIEW_CONNECTION, suggestedTargetConnectionId: null}],
                existingTargetId: 'existing-1',
                existingTargetName: 'Existing Collection',
            })
        );

        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Connections');

        expect(await screen.findByText('Unbound connections')).toBeInTheDocument();
        expect(screen.queryByText('Will be created disabled')).not.toBeInTheDocument();
    });

    it('keeps the dialog open and shows no success toast when the promotion fails', async () => {
        const user = userEvent.setup();

        mockMutateAsync.mockRejectedValue(new Error('promotion failed'));

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));

        expect(mockToast.success).not.toHaveBeenCalled();
        expect(onPromotedMock).not.toHaveBeenCalled();
        expect(screen.getByRole('button', {name: 'Promote'})).toBeInTheDocument();
    });

    it('calls onPromoted and switches the environment store when the toast action is clicked', async () => {
        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Confirm');

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

        expect(environmentStore.getState().currentEnvironmentId).toBe(0);

        action.onClick();

        await waitFor(() => expect(environmentStore.getState().currentEnvironmentId).toBe(1));

        expect(mockNavigate).toHaveBeenCalledWith('/automation/api-platform/api-collections', {state: undefined});
    });

    it.each([
        [PromotionResourceType.A2AServer, '/automation/a2a-servers'],
        [PromotionResourceType.McpServer, '/automation/mcp-servers'],
    ])(
        'navigates a promoted %s to the targetListPath prop in the target environment',
        async (resourceType, expectedPath) => {
            const user = userEvent.setup();

            mockPreview(makePreview({resourceType}));

            render(
                <EnvironmentPromotionDialog
                    {...defaultProps}
                    resourceType={resourceType}
                    targetListPath={expectedPath}
                />
            );

            await advanceToStep(user, 'Confirm');

            await user.click(screen.getByRole('button', {name: 'Promote'}));

            await waitFor(() => expect(mockToast.success).toHaveBeenCalledTimes(1));

            const [, toastOptions] = vi.mocked(mockToast.success).mock.calls[0];

            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (toastOptions as any).action.onClick();

            expect(environmentStore.getState().currentEnvironmentId).toBe(1);
            expect(mockNavigate).toHaveBeenCalledWith(expectedPath, {state: undefined});
        }
    );

    it('redirects a promoted project deployment to the target deployments page filtered by its project', async () => {
        const user = userEvent.setup();

        mockPreview(makePreview({resourceType: PromotionResourceType.ProjectDeployment}));

        mockMutateAsync.mockResolvedValue({
            promoteToEnvironment: {
                created: true,
                targetId: '42',
                targetUrl: null,
                unresolvedConnectionIds: [],
            },
        });

        render(
            <EnvironmentPromotionDialog
                {...defaultProps}
                resourceType={PromotionResourceType.ProjectDeployment}
                targetListPath="/automation/deployments"
            />
        );

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockToast.success).toHaveBeenCalledTimes(1));

        const [, toastOptions] = vi.mocked(mockToast.success).mock.calls[0];

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (toastOptions as any).action.onClick();

        expect(environmentStore.getState().currentEnvironmentId).toBe(1);
        expect(mockNavigate).toHaveBeenCalledWith('/automation/deployments?projectId=project-1', {
            state: {projectDeploymentId: 42},
        });
    });

    it('tells the user a created counterpart starts disabled even when every connection is bound', async () => {
        const user = userEvent.setup();

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockToast.success).toHaveBeenCalledTimes(1));

        const [, toastOptions] = vi.mocked(mockToast.success).mock.calls[0];

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((toastOptions as any).description).toContain('was created disabled in STAGING');
    });

    it('shows the new URL in the success toast when promoting an MCP server creates a counterpart', async () => {
        const user = userEvent.setup();

        mockPreview(makePreview({resourceType: PromotionResourceType.McpServer}));

        mockMutateAsync.mockResolvedValue({
            promoteToEnvironment: {
                created: true,
                targetId: 'target-1',
                targetUrl: 'https://example.com/api/automation/new-secret/mcp',
                unresolvedConnectionIds: [],
            },
        });

        render(<EnvironmentPromotionDialog {...defaultProps} resourceType={PromotionResourceType.McpServer} />);

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockToast.success).toHaveBeenCalledTimes(1));

        const [, toastOptions] = vi.mocked(mockToast.success).mock.calls[0];

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((toastOptions as any).description).toContain(
            'New URL: https://example.com/api/automation/new-secret/mcp'
        );
    });

    it('shows the new agent card URL in the success toast when promoting an A2A server creates a counterpart', async () => {
        const user = userEvent.setup();

        mockPreview(makePreview({resourceType: PromotionResourceType.A2AServer}));

        mockMutateAsync.mockResolvedValue({
            promoteToEnvironment: {
                created: true,
                targetId: 'target-1',
                targetUrl: 'https://example.com/api/automation/a2a/new-secret/.well-known/agent-card.json',
                unresolvedConnectionIds: [],
            },
        });

        render(<EnvironmentPromotionDialog {...defaultProps} resourceType={PromotionResourceType.A2AServer} />);

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockToast.success).toHaveBeenCalledTimes(1));

        const [, toastOptions] = vi.mocked(mockToast.success).mock.calls[0];

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((toastOptions as any).description).toContain(
            'New URL: https://example.com/api/automation/a2a/new-secret/.well-known/agent-card.json'
        );
    });

    it('does not show the new URL in the success toast when re-promoting updates an existing MCP server counterpart', async () => {
        const user = userEvent.setup();

        mockPreview(
            makePreview({
                existingTargetId: 'existing-mcp-1',
                existingTargetName: 'Existing MCP Server',
                resourceType: PromotionResourceType.McpServer,
            })
        );

        mockMutateAsync.mockResolvedValue({
            promoteToEnvironment: {
                created: false,
                targetId: 'existing-mcp-1',
                targetUrl: 'https://example.com/api/automation/unchanged-secret/mcp',
                unresolvedConnectionIds: [],
            },
        });

        render(<EnvironmentPromotionDialog {...defaultProps} resourceType={PromotionResourceType.McpServer} />);

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockToast.success).toHaveBeenCalledTimes(1));

        const [, toastOptions] = vi.mocked(mockToast.success).mock.calls[0];

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect((toastOptions as any).description).not.toContain('New URL:');
    });

    it('renders the schema changes only when the preview has some', async () => {
        const user = userEvent.setup();

        const {unmount} = render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Review changes');

        expect(screen.queryByText('Schema changes')).not.toBeInTheDocument();

        unmount();

        mockPreview(
            makePreview({
                schemaChanges: [
                    {
                        columnName: 'legacy',
                        destructive: true,
                        fromType: 'STRING',
                        kind: PromotionSchemaChangeKind.DropColumn,
                        tableName: 'orders',
                        toType: null,
                    },
                ],
            })
        );

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Review changes');

        expect(await screen.findByText('Schema changes')).toBeInTheDocument();
        expect(screen.getByText('Deletes data in this column')).toBeInTheDocument();
    });

    it('skips the Connections step when the preview has no connections', async () => {
        mockPreview(makePreview({connections: []}));

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        const stepsNavigation = await screen.findByRole('navigation', {name: 'Steps'});

        expect(within(stepsNavigation).getByRole('button', {name: /^Target/})).toBeInTheDocument();
        expect(within(stepsNavigation).getByRole('button', {name: /^Review changes/})).toBeInTheDocument();
        expect(within(stepsNavigation).getByRole('button', {name: /^Confirm/})).toBeInTheDocument();
        expect(within(stepsNavigation).queryByRole('button', {name: /^Connections/})).not.toBeInTheDocument();
    });

    it('disables Promote until destructive changes are acknowledged', async () => {
        const user = userEvent.setup();

        mockPreview(makePreview({changes: [DESTRUCTIVE_CHANGE], connections: []}));

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Confirm');

        expect(screen.getByRole('button', {name: 'Promote'})).toBeDisabled();

        await user.click(
            screen.getByRole('checkbox', {name: 'I understand 1 change(s) delete or overwrite data in STAGING'})
        );

        expect(screen.getByRole('button', {name: 'Promote'})).toBeEnabled();
    });

    it('resets the acknowledgement and returns to the first step when the target changes', async () => {
        const user = userEvent.setup();

        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockImplementation(
            (variables) =>
                ({
                    data: {
                        environmentPromotionPreview: makePreview({
                            changes: [DESTRUCTIVE_CHANGE],
                            connections: [],
                            destructiveChangesHash: `hash-${variables.targetEnvironmentId}`,
                            targetEnvironmentId: String(variables.targetEnvironmentId),
                        }),
                    },
                    isLoading: false,
                    refetch: mockRefetch,
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                }) as any
        );

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('checkbox'));

        expect(screen.getByRole('checkbox')).toBeChecked();

        const stepsNavigation = screen.getByRole('navigation', {name: 'Steps'});

        await user.click(within(stepsNavigation).getByRole('button', {name: /^Target/}));

        await user.click(screen.getByLabelText('Target environment'));

        await user.click(await screen.findByRole('option', {name: 'PRODUCTION'}));

        await waitFor(() =>
            expect(within(stepsNavigation).getByRole('button', {name: /^Confirm/})).toHaveAttribute(
                'aria-disabled',
                'true'
            )
        );

        expect(getCurrentStep()).toHaveTextContent('Target');
        expect(within(stepsNavigation).getByRole('button', {name: /^Review changes/})).toHaveAttribute(
            'aria-disabled',
            'true'
        );

        await advanceToStep(user, 'Confirm');

        expect(
            screen.getByRole('checkbox', {name: 'I understand 1 change(s) delete or overwrite data in PRODUCTION'})
        ).not.toBeChecked();
        expect(screen.getByRole('button', {name: 'Promote'})).toBeDisabled();
    });

    it('does not refetch the preview on window focus', async () => {
        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await waitFor(() =>
            expect(mockUseEnvironmentPromotionPreviewQuery).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    refetchOnReconnect: false,
                    refetchOnWindowFocus: false,
                    staleTime: Infinity,
                })
            )
        );
    });

    it('sends the destructive changes hash with promote', async () => {
        const user = userEvent.setup();

        mockPreview(
            makePreview({changes: [DESTRUCTIVE_CHANGE], connections: [], destructiveChangesHash: 'destructive-hash'})
        );

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('checkbox'));

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(1));

        expect(mockMutateAsync).toHaveBeenCalledWith({
            input: expect.objectContaining({expectedDestructiveChangesHash: 'destructive-hash'}),
        });
    });

    it('returns to review after a stale preview error', async () => {
        const user = userEvent.setup();

        let currentPreview = makePreview({changes: [DESTRUCTIVE_CHANGE], connections: []});

        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockImplementation(
            () =>
                ({
                    data: {environmentPromotionPreview: currentPreview},
                    isLoading: false,
                    refetch: mockRefetch,
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                }) as any
        );

        mockRefetch.mockImplementation(async () => {
            currentPreview = makePreview({
                changes: [DESTRUCTIVE_CHANGE, {...DESTRUCTIVE_CHANGE, key: 'tool-other', name: 'Other tool'}],
                connections: [],
                destructiveChangesHash: 'hash-2',
            });

            return {data: {environmentPromotionPreview: currentPreview}};
        });

        mockMutateAsync.mockRejectedValue(
            new GraphQLResponseError('The target changed since the preview; review the promotion again', {
                entityClass: 'EnvironmentPromotionErrorType',
                errorKey: 110,
            })
        );

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        await advanceToStep(user, 'Confirm');

        await user.click(screen.getByRole('checkbox'));

        await user.click(screen.getByRole('button', {name: 'Promote'}));

        await waitFor(() => expect(getCurrentStep()).toHaveTextContent('Review changes'));

        expect(mockRefetch).toHaveBeenCalledTimes(1);
        expect(screen.getByText('The target changed since the preview. Review the changes again.')).toBeInTheDocument();
        expect(screen.getByText('Other tool')).toBeInTheDocument();
        expect(mockToast.success).not.toHaveBeenCalled();

        await advanceToStep(user, 'Confirm');

        expect(
            screen.getByRole('checkbox', {name: 'I understand 2 change(s) delete or overwrite data in STAGING'})
        ).not.toBeChecked();
    });

    it('blocks the Target step when the preview fails', async () => {
        vi.mocked(mockUseEnvironmentPromotionPreviewQuery).mockReturnValue({
            data: undefined,
            error: new Error('The source is not promotable'),
            isError: true,
            isLoading: false,
            refetch: mockRefetch,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        render(<EnvironmentPromotionDialog {...defaultProps} />);

        expect(await screen.findByText('The source is not promotable')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
    });
});
