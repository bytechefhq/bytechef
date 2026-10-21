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

const renderTab = (canManage = true) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AppInstancesTab app={app} canManageApp={canManage} canManageInstance={() => canManage} />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('AppInstancesTab', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('lists the instances grouped by environment', () => {
        renderTab();

        const developmentGroup = screen.getByRole('region', {name: 'Development'});
        const productionGroup = screen.getByRole('region', {name: 'Production'});

        expect(within(developmentGroup).getByText('crm-dev')).toBeInTheDocument();
        expect(within(developmentGroup).queryByText('crm-prod')).not.toBeInTheDocument();
        expect(within(productionGroup).getByText('crm-prod')).toBeInTheDocument();
        expect(within(productionGroup).getByText('crm-germany')).toBeInTheDocument();
        expect(screen.queryByRole('region', {name: 'Staging'})).not.toBeInTheDocument();
    });

    it('links each instance of the app to its instance page', () => {
        renderTab(false);

        expect(screen.getByRole('link', {name: 'crm-dev'})).toHaveAttribute('href', '/automation/apps/instances/11');
        expect(screen.getByRole('link', {name: 'crm-prod'})).toHaveAttribute('href', '/automation/apps/instances/12');
        expect(screen.queryByRole('link', {name: 'crm-germany'})).not.toBeInTheDocument();
    });

    it('offers Suspend on an active instance and Activate on a suspended one', async () => {
        renderTab();

        await userEvent.click(screen.getByRole('button', {name: 'Suspend crm-dev'}));

        expect(updateStatusMutation.mutate).toHaveBeenCalledWith({id: '11', status: 'SUSPENDED'});

        await userEvent.click(screen.getByRole('button', {name: 'Activate crm-prod'}));

        expect(updateStatusMutation.mutate).toHaveBeenCalledWith({id: '12', status: 'ACTIVE'});
        expect(screen.queryByRole('button', {name: 'Activate crm-dev'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Suspend crm-prod'})).not.toBeInTheDocument();
    });

    it('hides the manage controls without APP_MANAGE', () => {
        renderTab(false);

        expect(screen.getByText('crm-dev')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New Instance'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Suspend/})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Activate/})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Edit/})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Rotate key/})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Delete/})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Configure/})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Share/})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /^Deployment settings/})).not.toBeInTheDocument();
    });

    it('offers Configure and Share on each instance the user may manage', () => {
        renderTab();

        expect(screen.getByRole('button', {name: 'Configure crm-dev'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Deployment settings of crm-dev'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Share crm-dev'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Share crm-prod'})).toBeInTheDocument();
        expect(within(screen.getByTestId('app-foreign-instance-row-13')).queryByRole('button')).not.toBeInTheDocument();
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

        await userEvent.click(screen.getByRole('button', {name: 'Rotate key of crm-dev'}));
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
