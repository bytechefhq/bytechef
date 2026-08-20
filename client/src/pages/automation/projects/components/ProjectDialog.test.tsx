import ProjectDialog from '@/pages/automation/projects/components/ProjectDialog';
import {Project} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {mockUseIsVisibilityEditionEnabled} = vi.hoisted(() => ({
    mockUseIsVisibilityEditionEnabled: vi.fn(),
}));

vi.mock('@/shared/hooks/useVisibilityFeatureEnabled', () => ({
    useIsVisibilityEditionEnabled: mockUseIsVisibilityEditionEnabled,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useWorkspaceStore: (selector: any) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/queries/automation/projectCategories.queries', () => ({
    ProjectCategoryKeys: {projectCategories: ['projectCategories']},
    useGetProjectCategoriesQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/projectTags.queries', () => ({
    ProjectTagKeys: {projectTags: (id: number) => ['projectTags', id]},
    useGetProjectTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

const existingProject = {
    id: 1,
    lastProjectVersion: 1,
    name: 'Alpha',
    visibility: 'WORKSPACE',
    workspaceId: 1,
} as never as Project;

let queryClient: QueryClient;

const Wrapper = ({children}: {children: ReactNode}) => (
    <MemoryRouter>
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </MemoryRouter>
);

beforeEach(() => {
    queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});
});

describe('ProjectDialog visibility', () => {
    it('offers the visibility picker when creating a project in EE', () => {
        mockUseIsVisibilityEditionEnabled.mockReturnValue(true);

        render(<ProjectDialog />, {wrapper: Wrapper});

        expect(screen.getByText('Visibility')).toBeInTheDocument();
        expect(screen.getByLabelText('Shared with workspace')).toBeInTheDocument();
        expect(screen.getByLabelText('Private')).toBeInTheDocument();
    });

    it('does not offer "Specific people" when creating, because there is no project id to grant against', () => {
        mockUseIsVisibilityEditionEnabled.mockReturnValue(true);

        render(<ProjectDialog />, {wrapper: Wrapper});

        // Anchor: the picker itself rendered, so the absence below is showSpecificPeopleOption={false} and not
        // a missing picker.
        expect(screen.getByLabelText('Shared with workspace')).toBeInTheDocument();
        expect(screen.queryByLabelText('Specific people')).not.toBeInTheDocument();
    });

    it('does not offer the picker when editing an existing project', () => {
        mockUseIsVisibilityEditionEnabled.mockReturnValue(true);

        render(<ProjectDialog project={existingProject} />, {wrapper: Wrapper});

        // Anchor: proves the dialog itself rendered, so the absence below is the gate and not a failed render.
        expect(screen.getByText('Edit Project')).toBeInTheDocument();
        expect(screen.queryByText('Visibility')).not.toBeInTheDocument();
    });

    it('does not offer the picker in CE', () => {
        mockUseIsVisibilityEditionEnabled.mockReturnValue(false);

        render(<ProjectDialog />, {wrapper: Wrapper});

        // Anchor: proves the dialog itself rendered, so the absence below is the gate and not a failed render.
        expect(screen.getByText('Create Project')).toBeInTheDocument();
        expect(screen.queryByText('Visibility')).not.toBeInTheDocument();
    });
});
