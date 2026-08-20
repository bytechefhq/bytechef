import ProjectVisibilityDialog from '@/pages/automation/project/components/ProjectVisibilityDialog';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {setProjectVisibilityMutateMock, useVisibilityFeatureEnabledMock} = vi.hoisted(() => ({
    setProjectVisibilityMutateMock: vi.fn(),
    useVisibilityFeatureEnabledMock: vi.fn(),
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useVisibilityFeatureEnabled: useVisibilityFeatureEnabledMock,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    ResourceVisibility: {Organization: 'ORGANIZATION', Private: 'PRIVATE', Workspace: 'WORKSPACE'},
    useGrantProjectAccessMutation: () => ({mutate: vi.fn()}),
    useProjectGrantsQuery: () => ({data: {projectGrants: []}}),
    useRevokeProjectAccessMutation: () => ({mutate: vi.fn()}),
    useSetProjectVisibilityMutation: () => ({mutate: setProjectVisibilityMutateMock}),
    useWorkspaceUsersQuery: () => ({data: {workspaceUsers: []}}),
}));

let queryClient: QueryClient;

const Wrapper = ({children}: {children: ReactNode}) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

beforeEach(() => {
    vi.clearAllMocks();

    queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    useVisibilityFeatureEnabledMock.mockReturnValue({enabled: true, isAdmin: false, workspaceId: 7});
});

describe('ProjectVisibilityDialog', () => {
    it('sends the picked reach for the project it was opened on', async () => {
        render(<ProjectVisibilityDialog onClose={vi.fn()} projectId={5} visibility="WORKSPACE" />, {wrapper: Wrapper});

        expect(screen.getByText('Project Visibility')).toBeInTheDocument();

        await userEvent.click(screen.getByLabelText('Private'));

        // Asserts the wiring end to end: the dialog's projectId reached the mutation through the shared hook.
        expect(setProjectVisibilityMutateMock).toHaveBeenCalledWith({
            projectId: '5',
            visibility: 'PRIVATE',
            workspaceId: '7',
        });
    });

    it('offers "Specific people" here, unlike the create dialog, because the project already exists', () => {
        render(<ProjectVisibilityDialog onClose={vi.fn()} projectId={5} visibility="PRIVATE" />, {wrapper: Wrapper});

        expect(screen.getByLabelText('Specific people')).toBeInTheDocument();
    });

    it('renders no picker when the visibility feature is off', () => {
        useVisibilityFeatureEnabledMock.mockReturnValue({enabled: false, isAdmin: false, workspaceId: undefined});

        render(<ProjectVisibilityDialog onClose={vi.fn()} projectId={5} visibility="WORKSPACE" />, {wrapper: Wrapper});

        // Anchor: the dialog shell rendered, so the absence below is the gate and not a failed render.
        expect(screen.getByText('Project Visibility')).toBeInTheDocument();
        expect(screen.queryByLabelText('Shared with workspace')).not.toBeInTheDocument();
    });
});
