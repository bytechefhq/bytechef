import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectListItem from '@/pages/automation/projects/components/project-list/ProjectListItem';
import {Project} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {mockUseVisibilityFeatureEnabled} = vi.hoisted(() => ({
    mockUseVisibilityFeatureEnabled: vi.fn(),
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useIsVisibilityEditionEnabled: () => false,
    useVisibilityFeatureEnabled: mockUseVisibilityFeatureEnabled,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAiDefaultModelQuery: () => ({data: undefined, isError: true, isPending: false}),
    useGrantProjectAccessMutation: () => ({mutate: vi.fn()}),
    useProjectGrantsQuery: () => ({data: {projectGrants: []}}),
    useRevokeProjectAccessMutation: () => ({mutate: vi.fn()}),
    useSetProjectVisibilityMutation: () => ({mutate: vi.fn()}),
    useWorkspaceUsersQuery: () => ({data: {workspaceUsers: []}}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useWorkspaceStore: (selector: any) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    EditionType: {CE: 'CE', EE: 'EE'},
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useApplicationInfoStore: (selector: any) =>
        selector({
            ai: {copilot: {enabled: true}},
            analytics: {enabled: false, postHog: {apiKey: '', host: ''}},
            application: {edition: 'EE'},
            featureFlags: {},
            templatesSubmissionForm: {projects: ''},
        }),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    ProjectDeploymentKeys: {projectDeployments: ['projectDeployments']},
    useGetWorkspaceProjectDeploymentsQuery: () => ({data: [], refetch: vi.fn()}),
}));

vi.mock('sonner', () => ({toast: vi.fn()}));

const project = {
    id: 1,
    lastProjectVersion: 1,
    name: 'Alpha',
    projectWorkflowIds: [],
    visibility: 'PRIVATE',
    workspaceId: 1,
} as never as Project;

let queryClient: QueryClient;

const Wrapper = ({children}: {children: ReactNode}) => (
    <MemoryRouter>
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <Collapsible>{children}</Collapsible>
            </TooltipProvider>
        </QueryClientProvider>
    </MemoryRouter>
);

beforeEach(() => {
    queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});
});

describe('ProjectListItem visibility', () => {
    it('renders the visibility badge in EE', () => {
        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 1});

        render(<ProjectListItem project={project} remainingTags={[]} />, {wrapper: Wrapper});

        expect(screen.getByLabelText('Change visibility')).toBeInTheDocument();
    });

    it('renders no badge in CE', () => {
        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: false, isAdmin: false, workspaceId: undefined});

        render(<ProjectListItem project={project} remainingTags={[]} />, {wrapper: Wrapper});

        // Anchor: proves the list item itself rendered, so the absence below is the gate and not a failed render.
        expect(screen.getByText('Alpha')).toBeInTheDocument();
        expect(screen.queryByLabelText('Change visibility')).not.toBeInTheDocument();
    });

    it('opens the picker in the dropdown without toggling the workflows collapsible', async () => {
        mockUseVisibilityFeatureEnabled.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 1});

        render(<ProjectListItem project={project} remainingTags={[]} />, {wrapper: Wrapper});

        const workflowsTrigger = screen.getByText('0 workflows').closest('button');

        expect(workflowsTrigger).toHaveAttribute('data-state', 'closed');

        await userEvent.click(screen.getByLabelText('Change visibility'));

        // The dropdown really renders renderVisibilityPicker's content, not just an empty menu.
        expect(screen.getByLabelText('Shared with workspace')).toBeInTheDocument();
        expect(screen.getByLabelText('Specific people')).toBeInTheDocument();

        // The badge sits inside the row whose onClick clicks the collapsible trigger; the stopPropagation
        // wrapper is what keeps opening the picker from also expanding the workflow list.
        expect(workflowsTrigger).toHaveAttribute('data-state', 'closed');
    });
});
