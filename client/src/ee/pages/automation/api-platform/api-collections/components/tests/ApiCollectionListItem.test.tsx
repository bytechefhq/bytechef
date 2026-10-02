import {ApiCollection} from '@/ee/shared/middleware/automation/api-platform';
import {act, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiCollectionListItem from '../ApiCollectionListItem';

const hoisted = vi.hoisted(() => ({
    deleteApiCollectionMock: vi.fn(),
    deleteOnSuccess: undefined as (() => void) | undefined,
}));

vi.mock('@/ee/shared/mutations/automation/apiCollections.mutations', () => ({
    useDeleteApiCollectionMutation: ({onSuccess}: {onSuccess: () => void}) => {
        hoisted.deleteOnSuccess = onSuccess;

        return {isPending: false, mutate: hoisted.deleteApiCollectionMock};
    },
}));

vi.mock('@/ee/shared/mutations/automation/apiCollectionTags.mutations', () => ({
    useUpdateApiCollectionTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/mutations/automation/projectDeployments.mutations', () => ({
    useEnableProjectDeploymentMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetProjectDeploymentQuery: () => ({data: undefined}),
}));

vi.mock('@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionListItemDropDownMenu', () => ({
    default: ({onDeleteClick}: {onDeleteClick: () => void}) => (
        <button onClick={onDeleteClick} type="button">
            Delete collection
        </button>
    ),
}));

vi.mock('@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionDialog', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionEndpointDialog', () => ({
    default: () => null,
}));

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: () => null,
}));

vi.mock('@/components/ui/collapsible', () => ({
    CollapsibleTrigger: ({children}: {children: React.ReactNode}) => <button type="button">{children}</button>,
}));

vi.mock('@/components/ui/tooltip', () => ({
    Tooltip: ({children}: {children: React.ReactNode}) => <>{children}</>,
    TooltipContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    TooltipTrigger: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

const apiCollection = {
    collectionVersion: 1,
    contextPath: 'orders',
    enabled: true,
    endpoints: [],
    id: 3,
    name: 'Orders API',
    projectDeploymentId: 9,
    projectId: 7,
    projectVersion: 2,
} as unknown as ApiCollection;

const openDeleteDialog = async () => {
    render(<ApiCollectionListItem apiCollection={apiCollection} />);

    await userEvent.click(screen.getByRole('button', {name: 'Delete collection'}));

    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('ApiCollectionListItem delete dialog', () => {
    it('deletes the collection on confirm and closes once the deletion succeeds', async () => {
        await openDeleteDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteApiCollectionMock).toHaveBeenCalledWith(3);
        expect(screen.getByRole('alertdialog')).toBeInTheDocument();

        act(() => hoisted.deleteOnSuccess?.());

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('closes without deleting when cancelled', async () => {
        await openDeleteDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(hoisted.deleteApiCollectionMock).not.toHaveBeenCalled();
    });
});
