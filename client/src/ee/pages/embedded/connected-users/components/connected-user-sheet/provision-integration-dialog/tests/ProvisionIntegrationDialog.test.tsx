import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useController} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProvisionIntegrationDialog from '../ProvisionIntegrationDialog';

const hoisted = vi.hoisted(() => ({
    connectionDefinitionQuery: vi.fn(),
    getProvisionableIntegrations: vi.fn(),
    provisionIntegrationInstance: vi.fn(),
    toastError: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: {error: hoisted.toastError},
}));

vi.mock('@/ee/shared/middleware/embedded/configuration', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    IntegrationInstanceApi: class {
        getProvisionableIntegrations = hoisted.getProvisionableIntegrations;
        provisionIntegrationInstance = hoisted.provisionIntegrationInstance;
    },
}));

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: (...args: unknown[]) => {
        hoisted.connectionDefinitionQuery(...args);

        return {
            data: {
                authorizations: [
                    {
                        properties: [
                            {controlType: 'TEXT', name: 'apiKey', type: 'STRING'},
                            {controlType: 'TEXT', name: 'clientId', type: 'STRING'},
                        ],
                        type: 'API_KEY',
                    },
                    {properties: [], type: 'OAUTH2_AUTHORIZATION_CODE'},
                ],
                properties: [
                    {controlType: 'TEXT', name: 'baseUri', type: 'STRING'},
                    {controlType: 'TEXT', name: 'sharedKey', type: 'STRING'},
                ],
            },
        };
    },
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: () => ({
        data: [
            {
                id: 'wf-1',
                inputs: [
                    {internalOnly: false, label: 'Channel', name: 'channel', required: true, type: 'string'},
                    {internalOnly: true, label: 'Secret', name: 'secret', type: 'string'},
                ],
                label: 'Notify',
                workflowUuid: 'u-1',
            },
        ],
    }),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => {
    const PropertyInput = ({control, name, required}: {control: never; name: string; required?: boolean}) => {
        const {field, fieldState} = useController({
            control,
            name,
            rules: {required: required ? 'This field is required.' : false},
        });

        return (
            <>
                <input aria-label={name.split('.').pop()} onChange={field.onChange} value={field.value ?? ''} />

                {fieldState.error && <span>{fieldState.error.message}</span>}
            </>
        );
    };

    return {
        default: ({
            control,
            controlPath,
            properties,
        }: {
            control: never;
            controlPath: string;
            properties: {name: string; required?: boolean}[];
        }) => (
            <div>
                {properties.map((property) => (
                    <PropertyInput
                        control={control}
                        key={property.name}
                        name={`${controlPath}.${property.name}`}
                        required={property.required}
                    />
                ))}
            </div>
        ),
    };
});

vi.mock('@/shared/components/connection/OAuth2Button', () => ({
    default: ({
        extraQueryParameters,
        onCodeSuccess,
    }: {
        extraQueryParameters?: Record<string, string>;
        onCodeSuccess: (payload: {code: string; code_verifier?: string}) => void;
    }) => (
        <button
            onClick={() => onCodeSuccess({code: 'auth-code', code_verifier: extraQueryParameters?.code_verifier})}
            type="button"
        >
            Authorize
        </button>
    ),
}));

const buildConfiguration = (id: number, authorizationType: string, selfServe: boolean) => ({
    integrationInstanceConfiguration: {
        authorizationType,
        id,
        integrationId: 1,
        integrationInstanceConfigurationWorkflows: [{workflowId: 'wf-1', workflowUuid: 'u-1'}],
        integrationVersion: id,
        name: `Config ${id}`,
    },
    oAuth2AuthorizationParameters: {authorizationUrl: 'https://auth.example.com', clientId: 'cid'},
    redirectUri: 'https://app.example.com/callback',
    selfServe,
    sharedConnectionParameterNames: ['clientId', 'sharedKey'],
});

const renderDialog = (onClose = vi.fn()) =>
    render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <ProvisionIntegrationDialog connectedUserId={7} onClose={onClose} />
        </QueryClientProvider>
    );

const chooseIntegration = async () => {
    await screen.findByRole('option', {name: 'Slack'});

    await userEvent.selectOptions(screen.getByRole('combobox', {name: 'Integration'}), '1');
};

describe('ProvisionIntegrationDialog', () => {
    beforeEach(() => {
        hoisted.getProvisionableIntegrations.mockReset();
        hoisted.provisionIntegrationInstance.mockReset();
        hoisted.toastError.mockReset();
        hoisted.connectionDefinitionQuery.mockReset();

        hoisted.provisionIntegrationInstance.mockResolvedValue(11);
        hoisted.getProvisionableIntegrations.mockResolvedValue([
            {
                componentVersion: 3,
                integration: {componentName: 'slack', id: 1, name: 'Slack'},
                integrationInstanceConfigurations: [
                    buildConfiguration(1, 'OAUTH2_AUTHORIZATION_CODE', false),
                    buildConfiguration(2, 'API_KEY', true),
                ],
            },
        ]);
    });

    it('lists only the provisionable configurations and marks the self-serve one', async () => {
        renderDialog();

        await chooseIntegration();

        expect(screen.getAllByRole('radio')).toHaveLength(2);
        expect(screen.getAllByText('Self-serve')).toHaveLength(1);
        expect(screen.getByText('Config 2 · v2')).toBeInTheDocument();
    });

    it('hides connection fields that the configuration already provides', async () => {
        renderDialog();

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 2/}));

        expect(screen.getByLabelText('apiKey')).toBeInTheDocument();
        expect(screen.queryByLabelText('clientId')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('sharedKey')).not.toBeInTheDocument();
    });

    it('submits the connection-level properties with the authorization properties', async () => {
        renderDialog();

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 2/}));
        await userEvent.type(screen.getByLabelText('baseUri'), 'https://api.example.com');
        await userEvent.type(screen.getByLabelText('apiKey'), 'k');
        await userEvent.click(screen.getByRole('button', {name: 'Provision'}));

        await waitFor(() => expect(hoisted.provisionIntegrationInstance).toHaveBeenCalled());

        expect(
            hoisted.provisionIntegrationInstance.mock.calls[0][0].provisionIntegrationInstanceRequest
                .connectionParameters
        ).toEqual({apiKey: 'k', baseUri: 'https://api.example.com'});
    });

    it('submits the chosen configuration, connection parameters and enabled workflows with inputs', async () => {
        const onClose = vi.fn();

        renderDialog(onClose);

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 2/}));
        await userEvent.type(screen.getByLabelText('apiKey'), 'k');

        expect(screen.queryByLabelText('channel')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('switch', {name: 'Notify'}));
        await userEvent.type(screen.getByLabelText('channel'), '#ops');

        expect(screen.queryByLabelText('secret')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Provision'}));

        await waitFor(() => expect(onClose).toHaveBeenCalled());

        expect(hoisted.provisionIntegrationInstance).toHaveBeenCalledWith({
            connectedUserId: 7,
            provisionIntegrationInstanceRequest: {
                connectionParameters: {apiKey: 'k'},
                integrationInstanceConfigurationId: 2,
                workflows: [{inputs: {channel: '#ops'}, workflowUuid: 'u-1'}],
            },
        });
    });

    it('blocks Provision with a field error while a required input of an enabled workflow is empty', async () => {
        renderDialog();

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 2/}));
        await userEvent.click(screen.getByRole('switch', {name: 'Notify'}));
        await userEvent.click(screen.getByRole('button', {name: 'Provision'}));

        expect(await screen.findByText('This field is required.')).toBeInTheDocument();
        expect(hoisted.provisionIntegrationInstance).not.toHaveBeenCalled();

        await userEvent.type(screen.getByLabelText('channel'), '#ops');
        await userEvent.click(screen.getByRole('button', {name: 'Provision'}));

        await waitFor(() => expect(hoisted.provisionIntegrationInstance).toHaveBeenCalled());
    });

    it('does not validate the required inputs of a workflow that was switched off again', async () => {
        renderDialog();

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 2/}));
        await userEvent.click(screen.getByRole('switch', {name: 'Notify'}));
        await userEvent.click(screen.getByRole('switch', {name: 'Notify'}));
        await userEvent.click(screen.getByRole('button', {name: 'Provision'}));

        await waitFor(() => expect(hoisted.provisionIntegrationInstance).toHaveBeenCalled());

        expect(
            hoisted.provisionIntegrationInstance.mock.calls[0][0].provisionIntegrationInstanceRequest.workflows
        ).toEqual([]);
    });

    it('keeps Provision disabled for an OAuth2 configuration until a code arrives', async () => {
        renderDialog();

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 1/}));

        expect(screen.getByRole('button', {name: 'Provision'})).toBeDisabled();

        await userEvent.click(screen.getByRole('button', {name: 'Authorize'}));

        expect(screen.getByRole('button', {name: 'Provision'})).toBeEnabled();
    });

    it('submits the authorization code and the PKCE code verifier of a PKCE configuration', async () => {
        const pkceConfiguration = buildConfiguration(3, 'OAUTH2_AUTHORIZATION_CODE_PKCE', false);

        hoisted.getProvisionableIntegrations.mockResolvedValue([
            {
                componentVersion: 3,
                integration: {componentName: 'slack', id: 1, name: 'Slack'},
                integrationInstanceConfigurations: [
                    {
                        ...pkceConfiguration,
                        oAuth2AuthorizationParameters: {
                            ...pkceConfiguration.oAuth2AuthorizationParameters,
                            extraQueryParameters: {
                                code_challenge: 'challenge-1',
                                code_challenge_method: 'S256',
                                code_verifier: 'verifier-1',
                            },
                        },
                    },
                ],
            },
        ]);

        renderDialog();

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 3/}));
        await userEvent.click(screen.getByRole('button', {name: 'Authorize'}));
        await userEvent.click(screen.getByRole('button', {name: 'Provision'}));

        await waitFor(() => expect(hoisted.provisionIntegrationInstance).toHaveBeenCalled());

        expect(
            hoisted.provisionIntegrationInstance.mock.calls[0][0].provisionIntegrationInstanceRequest
                .connectionParameters
        ).toEqual({code: 'auth-code', code_verifier: 'verifier-1'});
    });

    it('submits only the authorization code of a non-PKCE OAuth2 configuration', async () => {
        renderDialog();

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 1/}));
        await userEvent.click(screen.getByRole('button', {name: 'Authorize'}));
        await userEvent.click(screen.getByRole('button', {name: 'Provision'}));

        await waitFor(() => expect(hoisted.provisionIntegrationInstance).toHaveBeenCalled());

        expect(
            hoisted.provisionIntegrationInstance.mock.calls[0][0].provisionIntegrationInstanceRequest
                .connectionParameters
        ).toEqual({code: 'auth-code'});
    });

    it('keeps the dialog open and Provision enabled when provisioning fails', async () => {
        const onClose = vi.fn();

        hoisted.provisionIntegrationInstance.mockRejectedValue(new Error('Access denied'));

        renderDialog(onClose);

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 2/}));
        await userEvent.click(screen.getByRole('button', {name: 'Provision'}));

        await waitFor(() => expect(hoisted.provisionIntegrationInstance).toHaveBeenCalled());

        expect(hoisted.toastError).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
        expect(screen.getByRole('button', {name: 'Provision'})).toBeEnabled();
    });

    it('passes the integration component version to the connection definition query', async () => {
        renderDialog();

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 2/}));

        expect(hoisted.connectionDefinitionQuery).toHaveBeenCalledWith({componentName: 'slack', componentVersion: 3});
    });

    it('explains when nothing can be provisioned and keeps Provision disabled', async () => {
        hoisted.getProvisionableIntegrations.mockResolvedValue([]);

        renderDialog();

        expect(
            await screen.findByText('No integrations can be provisioned for this connected user.')
        ).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Integration'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Cancel'})).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Provision'})).toBeDisabled();
    });

    it('explains when the chosen configuration has no enabled workflows', async () => {
        hoisted.getProvisionableIntegrations.mockResolvedValue([
            {
                componentVersion: 3,
                integration: {componentName: 'slack', id: 1, name: 'Slack'},
                integrationInstanceConfigurations: [
                    {
                        ...buildConfiguration(2, 'API_KEY', true),
                        integrationInstanceConfiguration: {
                            ...buildConfiguration(2, 'API_KEY', true).integrationInstanceConfiguration,
                            integrationInstanceConfigurationWorkflows: [],
                        },
                    },
                ],
            },
        ]);

        renderDialog();

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 2/}));

        expect(screen.getByText('This configuration has no enabled workflows.')).toBeInTheDocument();
    });

    it('shows the OAuth2 misconfiguration message and keeps Provision disabled', async () => {
        hoisted.getProvisionableIntegrations.mockResolvedValue([
            {
                componentVersion: 3,
                integration: {componentName: 'slack', id: 1, name: 'Slack'},
                integrationInstanceConfigurations: [
                    {
                        ...buildConfiguration(1, 'OAUTH2_AUTHORIZATION_CODE', false),
                        oAuth2AuthorizationParameters: {clientId: 'cid'},
                    },
                ],
            },
        ]);

        renderDialog();

        await chooseIntegration();

        await userEvent.click(screen.getByRole('radio', {name: /Config 1/}));

        expect(
            screen.getByText("This configuration's OAuth2 app is missing its authorization URL or client ID.")
        ).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Provision'})).toBeDisabled();
    });
});
