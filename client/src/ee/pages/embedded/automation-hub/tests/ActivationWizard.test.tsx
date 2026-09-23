import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {AutomationWorkflowProjectWorkflowTemplate, ResponseError} from '@/ee/shared/middleware/embedded/public';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ActivationWizard from '../wizard/ActivationWizard';

const {
    copyTemplateMutateAsyncMock,
    deleteAutomationMutateAsyncMock,
    fetchWorkflowMock,
    navigateMock,
    publishMutateAsyncMock,
    refetchComponentDefinitionsMock,
    setEnabledMutateAsyncMock,
    updateInputsMutateAsyncMock,
    useGetComponentConnectionsQueryMock,
    useGetComponentDefinitionsQueryMock,
    wireNodeConnectionMutateAsyncMock,
} = vi.hoisted(() => ({
    copyTemplateMutateAsyncMock: vi.fn(),
    deleteAutomationMutateAsyncMock: vi.fn(),
    fetchWorkflowMock: vi.fn(),
    navigateMock: vi.fn(),
    publishMutateAsyncMock: vi.fn(),
    refetchComponentDefinitionsMock: vi.fn(),
    setEnabledMutateAsyncMock: vi.fn(),
    updateInputsMutateAsyncMock: vi.fn(),
    useGetComponentConnectionsQueryMock: vi.fn(),
    useGetComponentDefinitionsQueryMock: vi.fn(),
    wireNodeConnectionMutateAsyncMock: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {...actual, useNavigate: () => navigateMock};
});

vi.mock('@/ee/pages/embedded/automation-hub/queries/automationHub.queries', () => ({
    useFetchWorkflow: () => fetchWorkflowMock,
    useGetComponentConnectionsQuery: useGetComponentConnectionsQueryMock,
}));

vi.mock('@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations', () => ({
    useCopyTemplateMutation: () => ({mutateAsync: copyTemplateMutateAsyncMock}),
    useDeleteAutomationMutation: () => ({mutateAsync: deleteAutomationMutateAsyncMock}),
    usePublishAutomationMutation: () => ({mutateAsync: publishMutateAsyncMock}),
    useSetAutomationEnabledMutation: () => ({mutateAsync: setEnabledMutateAsyncMock}),
    useUpdateAutomationInputsMutation: () => ({mutateAsync: updateInputsMutateAsyncMock}),
    useWireNodeConnectionMutation: () => ({mutateAsync: wireNodeConnectionMutateAsyncMock}),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQueryMock,
}));

vi.mock('@/ee/pages/embedded/automation-hub/views/components/HubConnectionDialog', () => ({
    default: ({
        componentName,
        onClose,
        onCreated,
    }: {
        componentName: string;
        onClose: () => void;
        onCreated?: (id: number) => void;
    }) => (
        <div data-testid="hub-connection-dialog">
            <span data-testid="hub-connection-dialog-component">{componentName}</span>

            <button onClick={() => onCreated?.(42)} type="button">
                Create connection
            </button>

            <button onClick={onClose} type="button">
                Close connection dialog
            </button>
        </div>
    ),
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="component icon" src={src} />,
}));

const template: AutomationWorkflowProjectWorkflowTemplate = {
    components: [
        {icon: '<svg/>', name: 'slack', title: 'Slack'},
        {icon: '<svg/>', name: 'math', title: 'Math'},
    ],
    description: 'Send new lead updates to Slack',
    id: 'tpl-1',
    label: 'Slack sync',
};

const slackConnections = [
    {componentName: 'slack', id: 7, name: 'My Slack'},
    {componentName: 'slack', id: 8, name: 'My Other Slack'},
    {componentName: 'slack', id: 42, name: 'Brand new Slack'},
];

const copiedWorkflowDefinition = JSON.stringify({
    label: 'Slack sync',
    tasks: [
        {connections: [{key: 'slack'}], name: 'sendMessage_1', type: 'slack/v1/sendMessage'},
        {name: 'add_1', type: 'math/v1/add'},
    ],
    triggers: [],
});

const WIRING_ERROR_MESSAGE = 'Your accounts could not be connected to this automation. Please try again.';
const REQUIRED_COMPONENTS_ERROR_MESSAGE = 'This automation could not be set up. Please try again.';

const onCloseMock = vi.fn();

const renderWizard = (templateOverride: AutomationWorkflowProjectWorkflowTemplate = template) =>
    render(
        <MemoryRouter>
            <ActivationWizard onClose={onCloseMock} template={templateOverride} />
        </MemoryRouter>
    );

const selectConnection = (triggerLabel: string, connectionName: string) => {
    fireEvent.click(screen.getByLabelText(triggerLabel));
    fireEvent.click(screen.getByText(connectionName));
};

const clickButton = (name: string) => fireEvent.click(screen.getByRole('button', {name}));

const missingConnectionResponseError = (componentName: string) =>
    new ResponseError(
        new Response(JSON.stringify({missingConnectionComponentName: componentName}), {status: 409}),
        'Response returned an error code'
    );

const walkToActivateStep = (connectionName = 'My Slack') => {
    selectConnection('Slack connection', connectionName);

    clickButton('Next');
};

describe('ActivationWizard', () => {
    beforeEach(() => {
        copyTemplateMutateAsyncMock.mockReset();
        deleteAutomationMutateAsyncMock.mockReset();
        fetchWorkflowMock.mockReset();
        navigateMock.mockReset();
        onCloseMock.mockReset();
        publishMutateAsyncMock.mockReset();
        updateInputsMutateAsyncMock.mockReset();
        refetchComponentDefinitionsMock.mockReset();
        setEnabledMutateAsyncMock.mockReset();
        useGetComponentConnectionsQueryMock.mockReset();
        useGetComponentDefinitionsQueryMock.mockReset();
        wireNodeConnectionMutateAsyncMock.mockReset();

        copyTemplateMutateAsyncMock.mockResolvedValue('copy-1');
        deleteAutomationMutateAsyncMock.mockResolvedValue(undefined);
        fetchWorkflowMock.mockResolvedValue({definition: copiedWorkflowDefinition, workflowUuid: 'copy-1'});
        publishMutateAsyncMock.mockResolvedValue(undefined);
        refetchComponentDefinitionsMock.mockResolvedValue({});
        setEnabledMutateAsyncMock.mockResolvedValue(undefined);
        wireNodeConnectionMutateAsyncMock.mockResolvedValue(undefined);

        useGetComponentDefinitionsQueryMock.mockReturnValue({
            data: [{icon: '<svg/>', name: 'slack', title: 'Slack'}],
            error: null,
            isLoading: false,
            refetch: refetchComponentDefinitionsMock,
        });

        useAutomationHubStore.setState({
            connectionDialogAllowed: true,
            editWorkflowAllowed: true,
            includeComponents: undefined,
            initialized: true,
            tabs: {automations: true, connections: true, newWorkflow: true},
            theme: {},
        });

        useGetComponentConnectionsQueryMock.mockImplementation((componentName: string) => ({
            data: componentName === 'slack' ? slackConnections : [],
            error: null,
            isLoading: false,
        }));
    });

    it('writes nothing before Activate, which then copies, wires, publishes and enables', async () => {
        renderWizard();

        expect(screen.getByLabelText('Slack connection')).toBeInTheDocument();
        expect(screen.queryByLabelText('Math connection')).not.toBeInTheDocument();

        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

        walkToActivateStep();

        expect(copyTemplateMutateAsyncMock).not.toHaveBeenCalled();
        expect(wireNodeConnectionMutateAsyncMock).not.toHaveBeenCalled();
        expect(publishMutateAsyncMock).not.toHaveBeenCalled();
        expect(setEnabledMutateAsyncMock).not.toHaveBeenCalled();

        clickButton('Activate');

        await waitFor(() => expect(copyTemplateMutateAsyncMock).toHaveBeenCalledWith('tpl-1'));

        await waitFor(() =>
            expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledWith({
                connectionId: 7,
                workflowConnectionKey: 'slack',
                workflowNodeName: 'sendMessage_1',
                workflowUuid: 'copy-1',
            })
        );

        expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledTimes(1);

        await waitFor(() =>
            expect(setEnabledMutateAsyncMock).toHaveBeenCalledWith({enabled: true, workflowUuid: 'copy-1'})
        );

        expect(publishMutateAsyncMock).toHaveBeenCalledWith('copy-1');

        expect(wireNodeConnectionMutateAsyncMock.mock.invocationCallOrder[0]).toBeLessThan(
            publishMutateAsyncMock.mock.invocationCallOrder[0]
        );
        expect(publishMutateAsyncMock.mock.invocationCallOrder[0]).toBeLessThan(
            setEnabledMutateAsyncMock.mock.invocationCallOrder[0]
        );

        expect(deleteAutomationMutateAsyncMock).not.toHaveBeenCalled();

        expect(await screen.findByText('Your automation is running')).toBeInTheDocument();

        clickButton('Open in builder');

        expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/copy-1');
    });

    it('deletes the copy it made when the activation chain fails partway', async () => {
        wireNodeConnectionMutateAsyncMock.mockRejectedValueOnce(new Error('wiring rejected'));

        renderWizard();

        walkToActivateStep();

        clickButton('Activate');

        expect(await screen.findByText(WIRING_ERROR_MESSAGE)).toBeInTheDocument();

        await waitFor(() => expect(deleteAutomationMutateAsyncMock).toHaveBeenCalledWith('copy-1'));

        expect(publishMutateAsyncMock).not.toHaveBeenCalled();
        expect(setEnabledMutateAsyncMock).not.toHaveBeenCalled();
    });

    it('copies afresh on a retry rather than reusing the copy it just deleted', async () => {
        wireNodeConnectionMutateAsyncMock.mockRejectedValueOnce(new Error('wiring rejected'));

        renderWizard();

        walkToActivateStep();

        clickButton('Activate');

        await waitFor(() => expect(deleteAutomationMutateAsyncMock).toHaveBeenCalledWith('copy-1'));

        copyTemplateMutateAsyncMock.mockResolvedValue('copy-2');

        clickButton('Activate');

        await waitFor(() => expect(copyTemplateMutateAsyncMock).toHaveBeenCalledTimes(2));

        await waitFor(() =>
            expect(setEnabledMutateAsyncMock).toHaveBeenCalledWith({enabled: true, workflowUuid: 'copy-2'})
        );
    });

    it('spares a copy the user opened in the builder when a later activation fails', async () => {
        wireNodeConnectionMutateAsyncMock.mockRejectedValueOnce(new Error('wiring rejected'));

        renderWizard();

        clickButton('Edit workflow');

        await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/copy-1'));

        walkToActivateStep();

        clickButton('Activate');

        expect(await screen.findByText(WIRING_ERROR_MESSAGE)).toBeInTheDocument();

        expect(deleteAutomationMutateAsyncMock).not.toHaveBeenCalled();
    });

    it('drives the highlight loop when enabling reports a missing connection', async () => {
        setEnabledMutateAsyncMock.mockRejectedValueOnce(missingConnectionResponseError('slack'));

        renderWizard();

        walkToActivateStep();

        clickButton('Activate');

        expect(await screen.findByText('Connect Slack to continue')).toBeInTheDocument();

        await waitFor(() => expect(deleteAutomationMutateAsyncMock).toHaveBeenCalledWith('copy-1'));

        selectConnection('Slack connection', 'My Other Slack');

        clickButton('Next');
        clickButton('Activate');

        await waitFor(() =>
            expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledWith({
                connectionId: 8,
                workflowConnectionKey: 'slack',
                workflowNodeName: 'sendMessage_1',
                workflowUuid: 'copy-1',
            })
        );
    });

    it('reports progress while the chain runs and refuses a second Activate click', async () => {
        let resolveWiring: () => void = () => undefined;

        wireNodeConnectionMutateAsyncMock.mockImplementationOnce(
            () =>
                new Promise<void>((resolve) => {
                    resolveWiring = resolve;
                })
        );

        renderWizard();

        walkToActivateStep();

        clickButton('Activate');

        await waitFor(() => expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledTimes(1));

        expect(screen.getByText('Setting up your automation…')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Activate'})).toBeDisabled();

        await act(async () => {
            resolveWiring();
        });

        expect(await screen.findByText('Your automation is running')).toBeInTheDocument();
    });

    it('copies the template and opens the builder when Edit workflow is used on the connect step', async () => {
        renderWizard();

        clickButton('Edit workflow');

        await waitFor(() => expect(copyTemplateMutateAsyncMock).toHaveBeenCalledWith('tpl-1'));

        await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/copy-1'));
    });

    it('activates the copy Edit workflow already made rather than copying a second time', async () => {
        renderWizard();

        clickButton('Edit workflow');

        await waitFor(() => expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/copy-1'));

        walkToActivateStep();

        clickButton('Activate');

        await waitFor(() =>
            expect(setEnabledMutateAsyncMock).toHaveBeenCalledWith({enabled: true, workflowUuid: 'copy-1'})
        );

        expect(copyTemplateMutateAsyncMock).toHaveBeenCalledTimes(1);
    });

    it('offers no Edit workflow when the vendor has withdrawn it', () => {
        useAutomationHubStore.setState({editWorkflowAllowed: false});

        renderWizard();

        expect(screen.queryByRole('button', {name: 'Edit workflow'})).not.toBeInTheDocument();
    });

    it('opens HubConnectionDialog for the row component and selects the connection it creates', () => {
        renderWizard();

        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

        clickButton('Add a new Slack connection');

        expect(screen.getByTestId('hub-connection-dialog-component')).toHaveTextContent('slack');

        clickButton('Create connection');

        expect(screen.queryByTestId('hub-connection-dialog')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
    });

    it('replaces a stale failure message with the fresh missing-connection highlight', async () => {
        setEnabledMutateAsyncMock
            .mockRejectedValueOnce(new Error('Enabling blew up'))
            .mockRejectedValueOnce(missingConnectionResponseError('slack'));

        renderWizard();

        walkToActivateStep();

        clickButton('Activate');

        expect(await screen.findByText('Enabling blew up')).toBeInTheDocument();

        clickButton('Activate');

        expect(await screen.findByText('Connect Slack to continue')).toBeInTheDocument();
        expect(screen.queryByText('Enabling blew up')).not.toBeInTheDocument();
    });

    it('does not surface the SDK response-error boilerplate to the connected user', async () => {
        setEnabledMutateAsyncMock.mockRejectedValueOnce(
            new ResponseError(new Response('{}', {status: 500}), 'Response returned an error code')
        );

        renderWizard();

        walkToActivateStep();

        clickButton('Activate');

        expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
        expect(screen.queryByText('Response returned an error code')).not.toBeInTheDocument();
    });

    it('skips the connect step when no template component needs a connection', async () => {
        useGetComponentDefinitionsQueryMock.mockReturnValue({data: [], error: null, isLoading: false});

        renderWizard();

        expect(screen.queryByLabelText('Slack connection')).not.toBeInTheDocument();
        expect(copyTemplateMutateAsyncMock).not.toHaveBeenCalled();

        expect(screen.queryByRole('button', {name: 'Next'})).not.toBeInTheDocument();

        clickButton('Activate');

        await waitFor(() => expect(copyTemplateMutateAsyncMock).toHaveBeenCalledWith('tpl-1'));

        expect(wireNodeConnectionMutateAsyncMock).not.toHaveBeenCalled();
    });

    it('wires a copied node that declares no connections under the component name as key', async () => {
        fetchWorkflowMock.mockResolvedValue({
            definition: JSON.stringify({tasks: [{name: 'sendMessage_1', type: 'slack/v1/sendMessage'}]}),
            workflowUuid: 'copy-1',
        });

        renderWizard();

        walkToActivateStep();

        clickButton('Activate');

        await waitFor(() =>
            expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledWith({
                connectionId: 7,
                workflowConnectionKey: 'slack',
                workflowNodeName: 'sendMessage_1',
                workflowUuid: 'copy-1',
            })
        );
    });

    it('reports a failed read of the copy as a wiring failure and rolls the copy back', async () => {
        fetchWorkflowMock.mockRejectedValue(new Error('workflow load failed'));

        renderWizard();

        walkToActivateStep();

        clickButton('Activate');

        expect(await screen.findByText(WIRING_ERROR_MESSAGE)).toBeInTheDocument();

        await waitFor(() => expect(deleteAutomationMutateAsyncMock).toHaveBeenCalledWith('copy-1'));

        expect(wireNodeConnectionMutateAsyncMock).not.toHaveBeenCalled();
    });

    it('reports an unreadable copied definition in its own words rather than as a wiring failure', async () => {
        fetchWorkflowMock.mockResolvedValue({definition: '{not json', workflowUuid: 'copy-1'});

        renderWizard();

        walkToActivateStep();

        clickButton('Activate');

        expect(await screen.findByText('The copied automation could not be read.')).toBeInTheDocument();

        await waitFor(() => expect(deleteAutomationMutateAsyncMock).toHaveBeenCalledWith('copy-1'));
    });

    it('still reports the activation failure when the rollback itself fails', async () => {
        wireNodeConnectionMutateAsyncMock.mockRejectedValueOnce(new Error('wiring rejected'));
        deleteAutomationMutateAsyncMock.mockRejectedValue(new Error('delete blew up'));

        renderWizard();

        walkToActivateStep();

        clickButton('Activate');

        expect(await screen.findByText(WIRING_ERROR_MESSAGE)).toBeInTheDocument();
        expect(screen.queryByText('delete blew up')).not.toBeInTheDocument();
    });

    it('refuses to start activation when the component-definition lookup fails', () => {
        useGetComponentDefinitionsQueryMock.mockReturnValue({
            data: undefined,
            error: new Error('definitions unavailable'),
            isLoading: false,
            refetch: refetchComponentDefinitionsMock,
        });

        renderWizard();

        expect(screen.getByText(REQUIRED_COMPONENTS_ERROR_MESSAGE)).toBeInTheDocument();

        expect(screen.queryByLabelText('Slack connection')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Next'})).not.toBeInTheDocument();
        expect(copyTemplateMutateAsyncMock).not.toHaveBeenCalled();

        clickButton('Try again');

        expect(refetchComponentDefinitionsMock).toHaveBeenCalledTimes(1);
    });

    it('leaves an open wizard alone when a background refetch fails after data already loaded', () => {
        const {rerender} = renderWizard();

        expect(screen.getByLabelText('Slack connection')).toBeInTheDocument();

        selectConnection('Slack connection', 'My Slack');

        useGetComponentDefinitionsQueryMock.mockReturnValue({
            data: [{icon: '<svg/>', name: 'slack', title: 'Slack'}],
            error: new Error('background refetch failed'),
            isLoading: false,
            refetch: refetchComponentDefinitionsMock,
        });

        rerender(
            <MemoryRouter>
                <ActivationWizard onClose={onCloseMock} template={template} />
            </MemoryRouter>
        );

        expect(screen.getByLabelText('Slack connection')).toBeInTheDocument();
        expect(screen.getByText('My Slack')).toBeInTheDocument();
        expect(screen.queryByTestId('activation-wizard-error')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
    });

    it('offers no Connect button when the vendor disallowed the connection dialog', () => {
        useAutomationHubStore.setState({connectionDialogAllowed: false});

        renderWizard();

        expect(screen.getByLabelText('Slack connection')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add a new Slack connection'})).not.toBeInTheDocument();
    });

    describe('configure step', () => {
        const templateWithInputs: AutomationWorkflowProjectWorkflowTemplate = {
            ...template,
            inputs: [
                {label: 'Spreadsheet name', name: 'sheetName', required: true, type: 'STRING'},
                {label: 'Row limit', name: 'rowLimit', required: false, type: 'INTEGER'},
            ],
        };

        it('is not offered at all when the template declares no inputs', () => {
            renderWizard();

            selectConnection('Slack connection', 'My Slack');

            clickButton('Next');

            expect(screen.queryByText('Configure')).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Activate'})).toBeInTheDocument();
        });

        it('asks for the declared inputs between connecting and activating', () => {
            renderWizard(templateWithInputs);

            selectConnection('Slack connection', 'My Slack');

            clickButton('Next');

            expect(screen.getByLabelText(/Spreadsheet name/)).toBeInTheDocument();
            expect(screen.getByLabelText(/Row limit/)).toHaveAttribute('type', 'number');
            expect(screen.queryByRole('button', {name: 'Activate'})).not.toBeInTheDocument();
        });

        it('blocks Next until every required input has a value', () => {
            renderWizard(templateWithInputs);

            selectConnection('Slack connection', 'My Slack');

            clickButton('Next');

            expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

            fireEvent.change(screen.getByLabelText(/Spreadsheet name/), {target: {value: 'Leads'}});

            expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
        });

        it('stores the values between publishing and enabling', async () => {
            renderWizard(templateWithInputs);

            selectConnection('Slack connection', 'My Slack');

            clickButton('Next');

            fireEvent.change(screen.getByLabelText(/Spreadsheet name/), {target: {value: 'Leads'}});

            clickButton('Next');
            clickButton('Activate');

            await waitFor(() =>
                expect(updateInputsMutateAsyncMock).toHaveBeenCalledWith({
                    inputs: {sheetName: 'Leads'},
                    workflowUuid: 'copy-1',
                })
            );

            expect(publishMutateAsyncMock.mock.invocationCallOrder[0]).toBeLessThan(
                updateInputsMutateAsyncMock.mock.invocationCallOrder[0]
            );
            expect(updateInputsMutateAsyncMock.mock.invocationCallOrder[0]).toBeLessThan(
                setEnabledMutateAsyncMock.mock.invocationCallOrder[0]
            );
        });

        it('does not call the inputs endpoint for a template that declares none', async () => {
            renderWizard();

            walkToActivateStep();

            clickButton('Activate');

            await waitFor(() => expect(setEnabledMutateAsyncMock).toHaveBeenCalled());

            expect(updateInputsMutateAsyncMock).not.toHaveBeenCalled();
        });
    });
});
