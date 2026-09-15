import {ApiCollectionEndpoint} from '@/ee/shared/middleware/automation/api-platform';
import {ProjectDeploymentWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiCollectionEndpointListItem from '../ApiCollectionEndpointListItem';

const hoistedScope = vi.hoisted(() => ({grantedScopes: [] as string[]}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) =>
        hoistedScope.grantedScopes.includes(scope),
}));

vi.mock('@/ee/shared/mutations/automation/apiCollectionEndpoints.mutations', () => ({
    useDeleteApiCollectionEndpointMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/automation/apiCollections.queries', () => ({
    ApiCollectionKeys: {apiCollections: ['apiCollections']},
}));

vi.mock('@/shared/mutations/automation/projectDeploymentWorkflows.mutations', () => ({
    useEnableProjectDeploymentWorkflowMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/components/read-only-workflow-editor/hooks/useReadOnlyWorkflow', () => ({
    default: () => ({openReadOnlyWorkflowSheet: vi.fn()}),
}));

vi.mock('@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionEndpointDialog', () => ({
    default: () => null,
}));

const renderEndpointListItem = () =>
    render(
        <ApiCollectionEndpointListItem
            apiCollectionEndpoint={
                {
                    enabled: true,
                    httpMethod: 'GET',
                    id: 1,
                    name: 'Get orders',
                    path: 'orders',
                    workflowUuid: 'workflow-uuid',
                } as ApiCollectionEndpoint
            }
            collectionVersion={1}
            contextPath="shop"
            projectDeploymentId={1}
            projectDeploymentWorkflow={{id: 1} as ProjectDeploymentWorkflow}
            projectId={1}
            projectVersion={1}
            workflows={[{id: 'workflow-id', workflowUuid: 'workflow-uuid'} as Workflow]}
        />
    );

beforeEach(() => {
    hoistedScope.grantedScopes = ['API_PLATFORM_EDIT', 'DEPLOYMENT_EDIT'];

    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('ApiCollectionEndpointListItem', () => {
    it('should show every action and an enabled switch when all scopes are granted', async () => {
        const user = userEvent.setup();

        renderEndpointListItem();

        expect(screen.getByRole('switch', {name: 'Enable API Endpoint'})).toBeEnabled();

        await user.click(screen.getByRole('button', {name: 'API Endpoint actions'}));

        expect(screen.getByRole('menuitem', {name: 'Edit Endpoint'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Edit Workflow'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('should hide Edit Endpoint and Delete without API_PLATFORM_EDIT', async () => {
        hoistedScope.grantedScopes = ['DEPLOYMENT_EDIT'];

        const user = userEvent.setup();

        renderEndpointListItem();

        await user.click(screen.getByRole('button', {name: 'API Endpoint actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Edit Endpoint'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Edit Workflow'})).toBeInTheDocument();
    });

    it('should disable the switch and hide the menu for a member without any scope', () => {
        hoistedScope.grantedScopes = [];

        renderEndpointListItem();

        expect(screen.getByRole('switch', {name: 'Enable API Endpoint'})).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'API Endpoint actions'})).not.toBeInTheDocument();
    });
});
