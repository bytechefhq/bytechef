import {TooltipProvider} from '@/components/ui/tooltip';
import {Project} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import ProjectDeployments from './ProjectDeployments';

const publishedProject: Project = {
    id: 4,
    lastPublishedDate: new Date('2026-10-01T10:00:00Z'),
    name: 'Order alerts',
    projectWorkflowIds: [41],
    workspaceId: 1,
};

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {application: {edition: string}}) => unknown) =>
        selector({application: {edition: 'CE'}}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useEnvironmentsQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/projects.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projects.queries')>()),
    useGetWorkspaceProjectsQuery: () => ({data: [publishedProject], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/queries/automation/projectDeployments.queries')>()),
    useGetWorkspaceProjectDeploymentsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/projectDeploymentTags.queries', () => ({
    useGetProjectDeploymentTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: ({triggerNode}: {triggerNode: ReactNode}) => triggerNode,
}));

const renderProjectDeployments = (initialEntries: string[] = ['/']) =>
    render(
        <MemoryRouter initialEntries={initialEntries}>
            <TooltipProvider>
                <ProjectDeployments />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('ProjectDeployments empty states', () => {
    it('guides a first deployment and hides the project and tag sidebar', () => {
        renderProjectDeployments();

        expect(screen.getByRole('heading', {level: 2, name: 'Create your first deployment'})).toBeInTheDocument();
        expect(screen.queryByText('All Projects')).not.toBeInTheDocument();
        expect(screen.getByText('Deployments')).toBeInTheDocument();
    });

    it('keeps the sidebar and reports a filter miss when a filter matches nothing', () => {
        renderProjectDeployments(['/?tagId=1']);

        expect(screen.getByText('No Matching Project Deployments')).toBeInTheDocument();
        expect(screen.queryByText('Create your first deployment')).not.toBeInTheDocument();
        expect(screen.getByText('All Projects')).toBeInTheDocument();
    });
});
