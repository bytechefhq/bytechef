import {
    fireEvent,
    mockScrollIntoView,
    render,
    screen,
    stubMutation,
    userEvent,
    waitFor,
    windowResizeObserver,
    within,
} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppInstanceDeploymentSettingsDialog from '../components/AppInstanceDeploymentSettingsDialog';

const hoisted = vi.hoisted(() => ({
    appInstance: {} as Record<string, unknown>,
    bindableConnections: [] as Array<Record<string, unknown>>,
    bindableConnectionsFetching: false,
    settingsFetching: false,
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppBindableConnectionsQuery: vi.fn(() =>
        hoisted.bindableConnectionsFetching
            ? {data: undefined, isFetching: true}
            : {data: {appBindableConnections: hoisted.bindableConnections}, isFetching: false}
    ),
    useAppInstanceDeploymentSettingsQuery: vi.fn(() => ({
        data: {appInstance: hoisted.appInstance},
        isFetching: hoisted.settingsFetching,
    })),
    useUpdateAppInstanceDeploymentSettingsMutation: vi.fn(),
}));

const {useAppBindableConnectionsQuery, useUpdateAppInstanceDeploymentSettingsMutation} =
    await import('@/shared/middleware/graphql');

const updateSettingsMutation = stubMutation(vi.mocked(useUpdateAppInstanceDeploymentSettingsMutation));

const connections = [
    {componentName: 'vercel', componentVersion: 1, connectionVersion: 1, id: '31', name: 'Vercel team'},
    {componentName: 'httpClient', componentVersion: 2, connectionVersion: 1, id: '32', name: 'Deploy endpoint'},
    {componentName: 'slack', componentVersion: 1, connectionVersion: 1, id: '33', name: 'Sales Slack'},
];

const instanceSettings = (overrides: Record<string, unknown>) => ({
    deploymentConnectionId: null,
    deploymentProviderAvailable: true,
    deploymentSettings: {},
    environmentId: '1',
    hostingTarget: 'VERCEL',
    id: '21',
    ...overrides,
});

const renderDialog = () =>
    render(
        <AppInstanceDeploymentSettingsDialog
            appInstance={{id: '21', name: 'crm-staging'}}
            onOpenChange={vi.fn()}
            open={true}
        />
    );

const chooseConnection = async (connectionName: string) => {
    await userEvent.click(screen.getByRole('combobox', {name: 'Connection'}));
    await userEvent.click(await screen.findByText(connectionName));
};

const typeSetting = (label: string, value: string) => {
    fireEvent.change(screen.getByLabelText(label), {target: {value}});
};

describe('AppInstanceDeploymentSettingsDialog', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        windowResizeObserver();
        mockScrollIntoView();

        hoisted.appInstance = instanceSettings({});
        hoisted.bindableConnections = connections;
        hoisted.bindableConnectionsFetching = false;
        hoisted.settingsFetching = false;
    });

    it('asks for the Vercel settings and offers only Vercel connections of the instance scope', async () => {
        renderDialog();

        expect(vi.mocked(useAppBindableConnectionsQuery)).toHaveBeenCalledWith(
            {appInstanceId: '21'},
            expect.objectContaining({enabled: true})
        );
        expect(screen.getByLabelText('Project ID')).toBeInTheDocument();
        expect(screen.getByLabelText('Team ID (optional)')).toBeInTheDocument();
        expect(screen.getByLabelText('Repository')).toBeInTheDocument();
        expect(screen.queryByLabelText('Deploy URL')).not.toBeInTheDocument();
        expect(screen.getByText(/The credential lives in the connection/)).toBeInTheDocument();
        expect(screen.queryByLabelText(/token/i)).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'Connection'}));

        expect(await screen.findByText('Vercel team')).toBeInTheDocument();
        expect(screen.queryByText('Deploy endpoint')).not.toBeInTheDocument();
        expect(screen.queryByText('Sales Slack')).not.toBeInTheDocument();
    });

    it('defaults the target to preview and does not offer production outside production', () => {
        renderDialog();

        expect(screen.getByRole('combobox', {name: 'Target'})).toHaveTextContent('preview');
        expect(screen.getByText('Only a production instance may deploy to the production target.')).toBeInTheDocument();
    });

    it('defaults the target of a production instance to production', () => {
        hoisted.appInstance = instanceSettings({environmentId: '2'});

        renderDialog();

        expect(screen.getByRole('combobox', {name: 'Target'})).toHaveTextContent('production');
    });

    it('requires the connection, the project and the repository in owner/name form', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Connection is required')).toBeInTheDocument();
        expect(screen.getByText('Project ID is required')).toBeInTheDocument();
        expect(screen.getByText('Repository is required')).toBeInTheDocument();

        typeSetting('Repository', 'just-a-name');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Repository must be in owner/name form')).toBeInTheDocument();
        expect(updateSettingsMutation.mutate).not.toHaveBeenCalled();
    });

    it('saves the Vercel connection and settings', async () => {
        renderDialog();

        await chooseConnection('Vercel team');

        typeSetting('Project ID', ' prj_123 ');
        typeSetting('Repository', 'acme/sales-app');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateSettingsMutation.mutate).toHaveBeenCalledWith({
                id: '21',
                input: {
                    deploymentConnectionId: '31',
                    deploymentSettings: {projectId: 'prj_123', repository: 'acme/sales-app'},
                },
            });
        });
    });

    it('sends a stored setting the form does not know back unchanged', async () => {
        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '31',
            deploymentSettings: {buildCommand: 'npm run build', projectId: 'prj_9', repository: 'acme/crm', retries: 3},
        });

        renderDialog();

        typeSetting('Project ID', 'prj_10');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateSettingsMutation.mutate).toHaveBeenCalledWith({
                id: '21',
                input: {
                    deploymentConnectionId: '31',
                    deploymentSettings: {
                        buildCommand: 'npm run build',
                        projectId: 'prj_10',
                        repository: 'acme/crm',
                        retries: 3,
                    },
                },
            });
        });
    });

    it('removes an optional setting the user emptied', async () => {
        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '31',
            deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm', teamId: 'team_1'},
        });

        renderDialog();

        typeSetting('Team ID (optional)', '');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateSettingsMutation.mutate).toHaveBeenCalledWith({
                id: '21',
                input: {deploymentConnectionId: '31', deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm'}},
            });
        });
    });

    it('keeps the settings of another hosting target when the target of the instance changed', async () => {
        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '32',
            deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm'},
            hostingTarget: 'SELF_HOSTED',
        });

        renderDialog();

        typeSetting('Deploy URL', 'https://deploy.example.com/hooks');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateSettingsMutation.mutate).toHaveBeenCalledWith({
                id: '21',
                input: {
                    deploymentConnectionId: '32',
                    deploymentSettings: {
                        deployUrl: 'https://deploy.example.com/hooks',
                        projectId: 'prj_9',
                        repository: 'acme/crm',
                    },
                },
            });
        });
    });

    it('shows a stored production target on a non-production instance with a warning and sends it back untouched', async () => {
        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '31',
            deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm', target: 'production'},
        });

        renderDialog();

        expect(screen.getByRole('combobox', {name: 'Target'})).toHaveTextContent('production');
        expect(screen.getByText(/does not allow the stored target "production"/)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateSettingsMutation.mutate).toHaveBeenCalledWith({
                id: '21',
                input: {
                    deploymentConnectionId: '31',
                    deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm', target: 'production'},
                },
            });
        });
    });

    it('does not reset an edited field when the instance is refetched while the dialog is open', () => {
        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '31',
            deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm'},
        });

        const {rerender} = renderDialog();

        typeSetting('Project ID', 'prj_edited');

        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '31',
            deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm'},
        });

        rerender(
            <AppInstanceDeploymentSettingsDialog
                appInstance={{id: '21', name: 'crm-staging'}}
                onOpenChange={vi.fn()}
                open={true}
            />
        );

        expect(screen.getByLabelText('Project ID')).toHaveValue('prj_edited');
    });

    it('initialises from the fresh data when reopened over stale cached data', () => {
        const dialogFor = (open: boolean, appInstance = {id: '21', name: 'crm-staging'}) => (
            <AppInstanceDeploymentSettingsDialog appInstance={appInstance} onOpenChange={vi.fn()} open={open} />
        );

        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '31',
            deploymentSettings: {projectId: 'prj_old', repository: 'acme/old'},
        });

        const {rerender} = render(dialogFor(false));

        hoisted.settingsFetching = true;

        rerender(dialogFor(true));

        expect(screen.getByText('Loading the deployment settings...')).toBeInTheDocument();
        expect(screen.queryByLabelText('Project ID')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();

        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '31',
            deploymentSettings: {projectId: 'prj_new', repository: 'acme/new'},
        });
        hoisted.settingsFetching = false;

        rerender(dialogFor(true));

        expect(screen.getByLabelText('Project ID')).toHaveValue('prj_new');
        expect(screen.getByLabelText('Repository')).toHaveValue('acme/new');
    });

    it('shows the values of another instance when the instance id changes', () => {
        const dialogFor = (id: string) => (
            <AppInstanceDeploymentSettingsDialog appInstance={{id, name: 'crm'}} onOpenChange={vi.fn()} open={true} />
        );

        hoisted.appInstance = instanceSettings({deploymentSettings: {projectId: 'prj_21', repository: 'acme/a'}});

        const {rerender} = render(dialogFor('21'));

        typeSetting('Project ID', 'prj_edited');

        hoisted.appInstance = instanceSettings({
            deploymentSettings: {projectId: 'prj_22', repository: 'acme/b'},
            id: '22',
        });

        rerender(dialogFor('22'));

        expect(screen.getByLabelText('Project ID')).toHaveValue('prj_22');
        expect(screen.getByLabelText('Repository')).toHaveValue('acme/b');
    });

    it('shows no wrong-kind hint while the connection list is loading', () => {
        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '33',
            deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm'},
        });
        hoisted.bindableConnectionsFetching = true;

        renderDialog();

        const connectionCombobox = screen.getByRole('combobox', {name: 'Connection'});

        expect(connectionCombobox).toHaveTextContent('Connection 33');
        expect(connectionCombobox).not.toHaveTextContent('may not be');
    });

    it('hints that a stored connection outside the list may be of the wrong kind', () => {
        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '33',
            deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm'},
        });
        hoisted.bindableConnections = [connections[0]];

        renderDialog();

        expect(screen.getByRole('combobox', {name: 'Connection'})).toHaveTextContent(
            'Connection 33 (may not be a Vercel connection)'
        );
    });

    it('shows the stored settings', () => {
        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '31',
            deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm', teamId: 'team_1'},
        });

        renderDialog();

        expect(screen.getByLabelText('Project ID')).toHaveValue('prj_9');
        expect(screen.getByLabelText('Team ID (optional)')).toHaveValue('team_1');
        expect(screen.getByLabelText('Repository')).toHaveValue('acme/crm');
        expect(screen.getByRole('combobox', {name: 'Connection'})).toHaveTextContent('Vercel team');
    });

    it('asks for the deploy hook URLs and explains the id placeholder and the defaults', () => {
        hoisted.appInstance = instanceSettings({hostingTarget: 'SELF_HOSTED'});

        renderDialog();

        expect(screen.queryByLabelText('Project ID')).not.toBeInTheDocument();

        typeSetting('Deploy URL', 'https://deploy.example.com/hooks/?token=public');

        expect(screen.getByLabelText('Status URL (optional)')).toHaveAttribute(
            'placeholder',
            'https://deploy.example.com/hooks/{id}?token=public'
        );
        expect(screen.getByLabelText('Cancel URL (optional)')).toHaveAttribute(
            'placeholder',
            'https://deploy.example.com/hooks/{id}/cancel?token=public'
        );
        expect(screen.getAllByText(/\{id\} is replaced with the deployment id/)).toHaveLength(2);
    });

    it('validates the deploy hook URLs', async () => {
        hoisted.appInstance = instanceSettings({hostingTarget: 'SELF_HOSTED'});

        renderDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Deploy URL is required')).toBeInTheDocument();

        typeSetting('Deploy URL', 'http://deploy.example.com/hooks');
        typeSetting('Status URL (optional)', 'https://deploy.example.com/hooks/status');
        typeSetting('Cancel URL (optional)', 'not a url {id}');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(
            await screen.findByText('Deploy URL must use https outside the Development environment')
        ).toBeInTheDocument();
        expect(screen.getByText('Status URL must contain {id}')).toBeInTheDocument();
        expect(screen.getByText('Cancel URL must be an absolute http or https URL')).toBeInTheDocument();
        expect(updateSettingsMutation.mutate).not.toHaveBeenCalled();
    });

    it('saves the deploy hook connection and only the URLs that are filled in', async () => {
        hoisted.appInstance = instanceSettings({hostingTarget: 'SELF_HOSTED'});

        renderDialog();

        await chooseConnection('Deploy endpoint');

        typeSetting('Deploy URL', 'https://deploy.example.com/hooks');
        typeSetting('Cancel URL (optional)', 'https://deploy.example.com/hooks/{id}/stop');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateSettingsMutation.mutate).toHaveBeenCalledWith({
                id: '21',
                input: {
                    deploymentConnectionId: '32',
                    deploymentSettings: {
                        cancelUrl: 'https://deploy.example.com/hooks/{id}/stop',
                        deployUrl: 'https://deploy.example.com/hooks',
                    },
                },
            });
        });
    });

    it('names the kind of connection needed when there is none', () => {
        hoisted.appInstance = instanceSettings({hostingTarget: 'SELF_HOSTED'});
        hoisted.bindableConnections = [connections[0]];

        renderDialog();

        expect(
            screen.getByText(
                'No HTTP Client connection is available in the workspace and environment of this instance. Create a HTTP Client connection there first.'
            )
        ).toBeInTheDocument();
    });

    it('removes the deployment configuration after confirmation', async () => {
        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '31',
            deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm'},
        });

        renderDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Remove deployment configuration'}));

        const confirmation = await screen.findByRole('alertdialog');

        expect(updateSettingsMutation.mutate).not.toHaveBeenCalled();

        await userEvent.click(within(confirmation).getByRole('button', {name: 'Remove'}));

        expect(updateSettingsMutation.mutate).toHaveBeenCalledWith({
            id: '21',
            input: {deploymentConnectionId: null, deploymentSettings: {}},
        });
    });

    it('offers no removal when nothing is configured', () => {
        renderDialog();

        expect(screen.queryByRole('button', {name: 'Remove deployment configuration'})).not.toBeInTheDocument();
    });

    it('is read-only when no provider is available for the hosting target', () => {
        hoisted.appInstance = instanceSettings({
            deploymentConnectionId: '31',
            deploymentProviderAvailable: false,
            deploymentSettings: {projectId: 'prj_9', repository: 'acme/crm'},
        });

        renderDialog();

        expect(
            screen.getByText(/No deployment provider is available for the Vercel hosting target/)
        ).toBeInTheDocument();
        expect(screen.getByLabelText('Project ID')).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'Save'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Remove deployment configuration'})).not.toBeInTheDocument();
    });
});
