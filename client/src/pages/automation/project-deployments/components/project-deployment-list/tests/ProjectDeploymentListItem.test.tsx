import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentListItem from '../ProjectDeploymentListItem';

const hoistedScope = vi.hoisted(() => ({
    grantedScopes: ['DEPLOYMENT_CREATE', 'DEPLOYMENT_DELETE', 'DEPLOYMENT_EDIT'] as string[],
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) =>
        hoistedScope.grantedScopes.includes(scope),
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: vi.fn()}),
    };
});

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectDeploymentEnabled: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/projectDeploymentTags.mutations', () => ({
    useUpdateProjectDeploymentTagsMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/projectDeployments.mutations', () => ({
    useDeleteProjectDeploymentMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableProjectDeploymentMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: () => <div data-testid="project-deployment-dialog" />,
}));

vi.mock('@/shared/components/TagList', () => ({
    default: ({readOnly}: {readOnly?: boolean}) => (
        <div data-read-only={readOnly ? 'true' : 'false'} data-testid="tag-list" />
    ),
}));

const projectDeployment: ProjectDeployment = {
    enabled: false,
    id: 1,
    name: 'Test Deployment',
    projectDeploymentWorkflows: [{enabled: true, id: 11}],
    projectVersion: 1,
    tags: [],
};

const renderProjectDeploymentListItem = () =>
    render(
        <TooltipProvider>
            <Collapsible>
                <ProjectDeploymentListItem projectDeployment={projectDeployment} />
            </Collapsible>
        </TooltipProvider>
    );

beforeEach(() => {
    hoistedScope.grantedScopes = ['DEPLOYMENT_CREATE', 'DEPLOYMENT_DELETE', 'DEPLOYMENT_EDIT'];
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ProjectDeploymentListItem', () => {
    it('enables the deployment switch and tag editing with DEPLOYMENT_EDIT', () => {
        renderProjectDeploymentListItem();

        expect(screen.getByRole('switch', {name: 'Enable Deployment'})).not.toBeDisabled();
        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'false');
    });

    it('disables the deployment switch and makes tags read-only without DEPLOYMENT_EDIT', () => {
        hoistedScope.grantedScopes = ['DEPLOYMENT_CREATE', 'DEPLOYMENT_DELETE'];

        renderProjectDeploymentListItem();

        expect(screen.getByRole('switch', {name: 'Enable Deployment'})).toBeDisabled();
        expect(screen.getByTestId('tag-list')).toHaveAttribute('data-read-only', 'true');
    });

    it('hides the actions menu when neither DEPLOYMENT_CREATE nor DEPLOYMENT_DELETE is granted', () => {
        hoistedScope.grantedScopes = ['DEPLOYMENT_EDIT'];

        renderProjectDeploymentListItem();

        expect(screen.queryByRole('button', {name: 'More Deployment Actions'})).not.toBeInTheDocument();
    });
});
