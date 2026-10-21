import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, stubMutation, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppInstancesTab from '../components/AppInstancesTab';

vi.mock('@/shared/queries/automation/workspaces.queries', () => ({
    useGetUserWorkspacesQuery: vi.fn(() => ({
        data: [
            {id: 7, name: 'Sales'},
            {id: 8, name: 'Germany'},
        ],
    })),
}));

vi.mock('@/pages/automation/apps/hooks/useAppManageScopes', () => ({
    default: vi.fn(() => () => true),
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useCreateAppInstanceMutation: vi.fn(),
    useDeleteAppInstanceMutation: vi.fn(),
    useRotateAppInstanceSecretKeyMutation: vi.fn(),
    useUpdateAppInstanceMutation: vi.fn(),
    useUpdateAppInstanceStatusMutation: vi.fn(),
}));

const {
    useCreateAppInstanceMutation,
    useDeleteAppInstanceMutation,
    useRotateAppInstanceSecretKeyMutation,
    useUpdateAppInstanceMutation,
    useUpdateAppInstanceStatusMutation,
} = await import('@/shared/middleware/graphql');

stubMutation(vi.mocked(useCreateAppInstanceMutation));
stubMutation(vi.mocked(useDeleteAppInstanceMutation));
stubMutation(vi.mocked(useUpdateAppInstanceMutation));

const rotateMutation = stubMutation(vi.mocked(useRotateAppInstanceSecretKeyMutation));
const updateStatusMutation = stubMutation(vi.mocked(useUpdateAppInstanceStatusMutation));

const developmentInstance = {
    environmentId: '0',
    hostingTarget: 'VERCEL',
    id: '11',
    name: 'crm-dev',
    secretKeyHint: 'abcd1234',
    status: 'ACTIVE',
    url: 'https://crm-dev.vercel.app',
    workspaceId: '7',
};

const productionInstance = {
    environmentId: '2',
    hostingTarget: 'SELF_HOSTED',
    id: '12',
    name: 'crm-prod',
    secretKeyHint: 'efgh5678',
    status: 'SUSPENDED',
    url: null,
    workspaceId: '7',
};

const foreignInstance = {
    currentAppDeploymentId: null,
    environmentId: '2',
    id: '13',
    name: 'crm-germany',
    status: 'ACTIVE',
    workspaceId: '8',
};

const app = {
    foreignInstances: [foreignInstance],
    id: '1',
    instances: [developmentInstance, productionInstance],
    name: 'crm',
    status: 'ACTIVE',
} as never;

const renderTab = (canManage = true, renderedApp = app) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AppInstancesTab app={renderedApp} canManageApp={canManage} canManageInstance={() => canManage} />
            </TooltipProvider>
        </MemoryRouter>
    );

const openInstanceMenu = async (instanceName: string) => {
    await userEvent.click(screen.getByRole('button', {name: `Actions for instance ${instanceName}`}));
};

describe('AppInstancesTab', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('lists the instances it is given without environment headings', () => {
        renderTab();

        expect(screen.getByText('crm-dev')).toBeInTheDocument();
        expect(screen.getByText('crm-prod')).toBeInTheDocument();
        expect(screen.getByText('crm-germany')).toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Development'})).not.toBeInTheDocument();
        expect(screen.queryByRole('heading', {name: 'Production'})).not.toBeInTheDocument();
    });

    it('links each instance of the app to its instance page', () => {
        renderTab(false);

        expect(screen.getByRole('link', {name: 'crm-dev'})).toHaveAttribute('href', '/automation/apps/instances/11');
        expect(screen.getByRole('link', {name: 'crm-prod'})).toHaveAttribute('href', '/automation/apps/instances/12');
        expect(screen.queryByRole('link', {name: 'crm-germany'})).not.toBeInTheDocument();
    });

    it('keeps the instance actions in a menu instead of inline buttons', () => {
        renderTab();

        const ownRow = screen.getByTestId('app-instance-row-11');

        expect(within(ownRow).getByRole('button', {name: 'Actions for instance crm-dev'})).toBeInTheDocument();
        expect(within(ownRow).queryByRole('button', {name: /^Suspend/})).not.toBeInTheDocument();
        expect(within(ownRow).queryByRole('button', {name: /^Delete/})).not.toBeInTheDocument();
    });

    it('shows the badges of an instance on the right, after its key and before its menu', () => {
        renderTab();

        const ownRow = screen.getByTestId('app-instance-row-11');
        const keyHint = within(ownRow).getByText('Key …abcd1234');
        const hostingBadge = within(ownRow).getByText('Vercel');
        const actionsButton = within(ownRow).getByRole('button', {name: 'Actions for instance crm-dev'});

        expect(keyHint.compareDocumentPosition(hostingBadge) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(hostingBadge.compareDocumentPosition(actionsButton) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('offers Suspend on an active instance and Activate on a suspended one', async () => {
        renderTab();

        await openInstanceMenu('crm-dev');

        expect(screen.queryByRole('menuitem', {name: 'Activate'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Suspend'}));

        expect(updateStatusMutation.mutate).toHaveBeenCalledWith({id: '11', status: 'SUSPENDED'});

        await openInstanceMenu('crm-prod');

        expect(screen.queryByRole('menuitem', {name: 'Suspend'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Activate'}));

        expect(updateStatusMutation.mutate).toHaveBeenCalledWith({id: '12', status: 'ACTIVE'});
    });

    it('shows a centered empty state with New Instance when the app has no instances', () => {
        renderTab(true, {foreignInstances: [], id: '1', instances: [], name: 'crm', status: 'ACTIVE'} as never);

        expect(screen.getByRole('heading', {name: 'No instances yet'})).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'New Instance'})).toHaveLength(1);
        expect(
            screen.queryByText('Each instance has its own environment, URL, key and bindings.')
        ).not.toBeInTheDocument();
    });

    it('hides the manage controls without APP_MANAGE', () => {
        renderTab(false);

        expect(screen.getByText('crm-dev')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New Instance'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Actions for instance/})).not.toBeInTheDocument();
    });

    it('offers every manage action in the menu of an instance the user may manage', async () => {
        renderTab();

        expect(screen.getByRole('button', {name: 'Actions for instance crm-prod'})).toBeInTheDocument();
        expect(within(screen.getByTestId('app-foreign-instance-row-13')).queryByRole('button')).not.toBeInTheDocument();

        await openInstanceMenu('crm-dev');

        expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent?.trim())).toEqual([
            'Suspend',
            'Edit',
            'Configure',
            'Deployment settings',
            'Share',
            'Rotate key',
            'Delete',
        ]);
    });

    it('shows a foreign instance without bindings, key or manage controls', () => {
        renderTab();

        const foreignRow = screen.getByTestId('app-foreign-instance-row-13');

        expect(within(foreignRow).getByText('crm-germany')).toBeInTheDocument();
        expect(within(foreignRow).getByText('Workspace: Germany')).toBeInTheDocument();
        expect(within(foreignRow).queryByText(/Key/)).not.toBeInTheDocument();
        expect(within(foreignRow).queryByRole('button')).not.toBeInTheDocument();

        const ownRow = screen.getByTestId('app-instance-row-11');

        expect(within(ownRow).getByText('Key …abcd1234')).toBeInTheDocument();
    });

    it('shows the new key once after a rotate', async () => {
        renderTab();

        await openInstanceMenu('crm-dev');
        await userEvent.click(screen.getByRole('menuitem', {name: 'Rotate key'}));
        await userEvent.click(await screen.findByRole('button', {name: 'Rotate key'}));

        expect(rotateMutation.mutate).toHaveBeenCalledWith({id: '11'});

        rotateMutation.triggerOnSuccess(
            {rotateAppInstanceSecretKey: {appInstance: developmentInstance, secretKey: 'rotatedsecretkey'}},
            {id: '11'}
        );

        await waitFor(() => {
            expect(screen.getByDisplayValue('rotatedsecretkey')).toBeInTheDocument();
        });
        expect(screen.getByText(/ByteChef cannot show it again/)).toBeInTheDocument();
    });
});
