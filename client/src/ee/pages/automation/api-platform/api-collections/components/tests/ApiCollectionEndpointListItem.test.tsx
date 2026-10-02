import {ApiCollectionEndpoint} from '@/ee/shared/middleware/automation/api-platform';
import {ProjectDeploymentWorkflow} from '@/shared/middleware/automation/configuration';
import {act, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiCollectionEndpointListItem from '../ApiCollectionEndpointListItem';

const hoisted = vi.hoisted(() => ({
    deleteApiCollectionEndpointMock: vi.fn(),
    deleteOnSuccess: undefined as (() => void) | undefined,
}));

vi.mock('@/ee/shared/mutations/automation/apiCollectionEndpoints.mutations', () => ({
    useDeleteApiCollectionEndpointMutation: ({onSuccess}: {onSuccess: () => void}) => {
        hoisted.deleteOnSuccess = onSuccess;

        return {isPending: false, mutate: hoisted.deleteApiCollectionEndpointMock};
    },
}));

vi.mock('@/shared/mutations/automation/projectDeploymentWorkflows.mutations', () => ({
    useEnableProjectDeploymentWorkflowMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/components/read-only-workflow-editor/hooks/useReadOnlyWorkflow', () => ({
    default: () => ({openReadOnlyWorkflowSheet: vi.fn()}),
}));

vi.mock('@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionEndpointDialog', () => ({
    default: () => null,
}));

vi.mock('@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog', () => ({
    default: () => null,
}));

vi.mock('@/components/ui/dropdown-menu', () => ({
    DropdownMenu: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuItem: ({children, onClick}: {children: React.ReactNode; onClick?: () => void}) => (
        <button onClick={onClick} type="button">
            {children}
        </button>
    ),
    DropdownMenuSeparator: () => <hr />,
    DropdownMenuTrigger: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

const apiCollectionEndpoint = {
    apiCollectionId: 3,
    enabled: true,
    httpMethod: 'GET',
    id: 11,
    name: 'List orders',
    path: 'orders',
    workflowUuid: 'workflow-uuid',
} as unknown as ApiCollectionEndpoint;

const openDeleteDialog = async () => {
    render(
        <ApiCollectionEndpointListItem
            apiCollectionEndpoint={apiCollectionEndpoint}
            collectionVersion={1}
            contextPath="orders"
            projectDeploymentId={9}
            projectDeploymentWorkflow={{} as ProjectDeploymentWorkflow}
            projectId={7}
            projectVersion={2}
            workflows={[]}
        />
    );

    await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('ApiCollectionEndpointListItem delete dialog', () => {
    it('deletes the endpoint on confirm and closes once the deletion succeeds', async () => {
        await openDeleteDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteApiCollectionEndpointMock).toHaveBeenCalledWith(11);
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();

        act(() => hoisted.deleteOnSuccess?.());

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('closes without deleting when cancelled', async () => {
        await openDeleteDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(hoisted.deleteApiCollectionEndpointMock).not.toHaveBeenCalled();
    });
});
