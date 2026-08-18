import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {
    AutomationWorkflowProjectKindEnum,
    AutomationWorkflowProjectWorkflowTemplate,
    ResponseError,
} from '@/ee/shared/middleware/embedded/public';
import {act, fireEvent, render, screen, waitFor} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ActivationWizard from '../wizard/ActivationWizard';

// vi.mock factories hoist above module-scope `const`s, so refs they close over must come from
// vi.hoisted — see CLAUDE.md's Vitest mock factory hoisting note.
const {
    copyTemplateMutateAsyncMock,
    deprovisionMutateAsyncMock,
    navigateMock,
    provisionMutateAsyncMock,
    publishMutateAsyncMock,
    refetchComponentDefinitionsMock,
    refetchWorkflowMock,
    setEnabledMutateAsyncMock,
    useGetComponentConnectionsQueryMock,
    useGetComponentDefinitionsQueryMock,
    useGetWorkflowQueryMock,
    wireNodeConnectionMutateAsyncMock,
} = vi.hoisted(() => ({
    copyTemplateMutateAsyncMock: vi.fn(),
    deprovisionMutateAsyncMock: vi.fn(),
    navigateMock: vi.fn(),
    provisionMutateAsyncMock: vi.fn(),
    publishMutateAsyncMock: vi.fn(),
    refetchComponentDefinitionsMock: vi.fn(),
    refetchWorkflowMock: vi.fn(),
    setEnabledMutateAsyncMock: vi.fn(),
    useGetComponentConnectionsQueryMock: vi.fn(),
    useGetComponentDefinitionsQueryMock: vi.fn(),
    useGetWorkflowQueryMock: vi.fn(),
    wireNodeConnectionMutateAsyncMock: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {...actual, useNavigate: () => navigateMock};
});

vi.mock('@/ee/pages/embedded/automation-hub/queries/automationHub.queries', () => ({
    useGetComponentConnectionsQuery: useGetComponentConnectionsQueryMock,
    useGetWorkflowQuery: useGetWorkflowQueryMock,
}));

vi.mock('@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations', () => ({
    useCopyTemplateMutation: () => ({mutateAsync: copyTemplateMutateAsyncMock}),
    useDeprovisionReferenceMutation: () => ({mutateAsync: deprovisionMutateAsyncMock}),
    useProvisionReferenceMutation: () => ({mutateAsync: provisionMutateAsyncMock}),
    usePublishAutomationMutation: () => ({mutateAsync: publishMutateAsyncMock}),
    useSetAutomationEnabledMutation: () => ({mutateAsync: setEnabledMutateAsyncMock}),
    useWireNodeConnectionMutation: () => ({mutateAsync: wireNodeConnectionMutateAsyncMock}),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQueryMock,
}));

// The real dialog stands up ConnectionDialog's whole query graph; the wizard only cares that it is
// opened for the right component and that `onCreated` feeds a SELECT_CONNECTION back in.
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

const workflowLoadError = new Error('workflow load failed');

const WIRING_ERROR_MESSAGE = 'Your accounts could not be connected to this automation. Please try again.';
const REQUIRED_COMPONENTS_ERROR_MESSAGE = 'This automation could not be set up. Please try again.';

const onCloseMock = vi.fn();

const renderWizard = (kind: AutomationWorkflowProjectKindEnum) =>
    render(
        <MemoryRouter>
            <ActivationWizard kind={kind} onClose={onCloseMock} template={template} />
        </MemoryRouter>
    );

// Radix's Select is driven with fireEvent rather than userEvent: inside a Radix Dialog the body
// carries `pointer-events: none`, which userEvent's pointer checks refuse to click through.
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

describe('ActivationWizard', () => {
    beforeEach(() => {
        copyTemplateMutateAsyncMock.mockReset();
        deprovisionMutateAsyncMock.mockReset();
        navigateMock.mockReset();
        onCloseMock.mockReset();
        provisionMutateAsyncMock.mockReset();
        publishMutateAsyncMock.mockReset();
        refetchComponentDefinitionsMock.mockReset();
        refetchWorkflowMock.mockReset();
        setEnabledMutateAsyncMock.mockReset();
        useGetComponentConnectionsQueryMock.mockReset();
        useGetComponentDefinitionsQueryMock.mockReset();
        useGetWorkflowQueryMock.mockReset();
        wireNodeConnectionMutateAsyncMock.mockReset();

        copyTemplateMutateAsyncMock.mockResolvedValue('copy-1');
        deprovisionMutateAsyncMock.mockResolvedValue(undefined);
        provisionMutateAsyncMock.mockResolvedValue(undefined);
        publishMutateAsyncMock.mockResolvedValue(undefined);
        refetchComponentDefinitionsMock.mockResolvedValue({});
        refetchWorkflowMock.mockResolvedValue({});
        setEnabledMutateAsyncMock.mockResolvedValue({});
        wireNodeConnectionMutateAsyncMock.mockResolvedValue(undefined);

        // Only `slack` carries a connection definition — `math` does not, so it never becomes a row.
        useGetComponentDefinitionsQueryMock.mockReturnValue({
            data: [{icon: '<svg/>', name: 'slack', title: 'Slack'}],
            error: null,
            isLoading: false,
            refetch: refetchComponentDefinitionsMock,
        });

        useAutomationHubStore.setState({
            connectionDialogAllowed: true,
            includeComponents: undefined,
            initialized: true,
            sharedConnectionIds: [],
            tabs: {automations: true, connections: true, newWorkflow: true},
            theme: {},
        });

        useGetComponentConnectionsQueryMock.mockImplementation((componentName: string) => ({
            data: componentName === 'slack' ? slackConnections : [],
            error: null,
            isLoading: false,
        }));

        useGetWorkflowQueryMock.mockImplementation((workflowUuid?: string) => ({
            data: workflowUuid === 'copy-1' ? {definition: copiedWorkflowDefinition, workflowUuid} : undefined,
            error: null,
            isLoading: false,
            refetch: refetchWorkflowMock,
        }));
    });

    it('walks the COPY path: connect, copy and wire, then publish before enabling', async () => {
        renderWizard('COPY');

        expect(screen.getByLabelText('Slack connection')).toBeInTheDocument();
        expect(screen.queryByLabelText('Math connection')).not.toBeInTheDocument();

        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

        selectConnection('Slack connection', 'My Slack');

        expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();

        clickButton('Next');

        await waitFor(() => expect(copyTemplateMutateAsyncMock).toHaveBeenCalledWith('tpl-1'));

        expect(provisionMutateAsyncMock).not.toHaveBeenCalled();

        await waitFor(() =>
            expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledWith({
                connectionId: 7,
                workflowConnectionKey: 'slack',
                workflowNodeName: 'sendMessage_1',
                workflowUuid: 'copy-1',
            })
        );

        // The `math` task has no selection, so it is never wired.
        expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledTimes(1);

        expect(await screen.findByText('My Slack')).toBeInTheDocument();

        // Next only opens once the wiring PUTs have settled — publishing before then would snapshot
        // an incomplete connection list into the deployment.
        await waitFor(() => expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled());

        clickButton('Next');

        clickButton('Activate');

        await waitFor(() =>
            expect(setEnabledMutateAsyncMock).toHaveBeenCalledWith({enabled: true, workflowUuid: 'copy-1'})
        );

        expect(publishMutateAsyncMock).toHaveBeenCalledWith('copy-1');
        expect(publishMutateAsyncMock.mock.invocationCallOrder[0]).toBeLessThan(
            setEnabledMutateAsyncMock.mock.invocationCallOrder[0]
        );

        expect(await screen.findByText('Your automation is running')).toBeInTheDocument();

        clickButton('Open in builder');

        expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/copy-1');
    });

    it('de-provisions before re-provisioning after a missing-connection 409, then enables without publishing', async () => {
        provisionMutateAsyncMock.mockRejectedValueOnce(missingConnectionResponseError('slack'));

        renderWizard('REFERENCE');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        await waitFor(() => expect(provisionMutateAsyncMock).toHaveBeenCalledTimes(1));

        // Nothing to clean up before the first attempt.
        expect(deprovisionMutateAsyncMock).not.toHaveBeenCalled();

        expect(await screen.findByText('Connect Slack to continue')).toBeInTheDocument();
        expect(screen.getByLabelText('Slack connection')).toHaveAttribute('aria-invalid', 'true');
        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

        selectConnection('Slack connection', 'My Other Slack');

        expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();

        clickButton('Next');

        await waitFor(() => expect(provisionMutateAsyncMock).toHaveBeenCalledTimes(2));

        // The 409 left a disabled reference row behind and `getOrCreateReference` returns it
        // unchanged, so the retry has to remove it first or it silently no-ops.
        expect(deprovisionMutateAsyncMock).toHaveBeenCalledTimes(1);
        expect(deprovisionMutateAsyncMock).toHaveBeenCalledWith('tpl-1');
        expect(deprovisionMutateAsyncMock.mock.invocationCallOrder[0]).toBeLessThan(
            provisionMutateAsyncMock.mock.invocationCallOrder[1]
        );

        expect(wireNodeConnectionMutateAsyncMock).not.toHaveBeenCalled();

        clickButton('Next');

        clickButton('Activate');

        await waitFor(() =>
            expect(setEnabledMutateAsyncMock).toHaveBeenCalledWith({enabled: true, workflowUuid: 'tpl-1'})
        );

        expect(publishMutateAsyncMock).not.toHaveBeenCalled();

        expect(await screen.findByText('Your automation is running')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Open in builder'})).not.toBeInTheDocument();
    });

    it('opens HubConnectionDialog for the row component and selects the connection it creates', () => {
        renderWizard('COPY');

        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

        clickButton('Add a new Slack connection');

        expect(screen.getByTestId('hub-connection-dialog-component')).toHaveTextContent('slack');

        clickButton('Create connection');

        expect(screen.queryByTestId('hub-connection-dialog')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
    });

    it('replaces a stale failure message with the fresh missing-connection highlight', async () => {
        // A rolled-back provision leaves nothing to remove, so the server would answer a
        // de-provision with WORKFLOW_NOT_FOUND. Rejecting it here keeps the test honest: if the
        // retry de-provisioned, this path would dead-end instead of reaching the 409.
        deprovisionMutateAsyncMock.mockRejectedValue(new Error('Workflow not found'));

        provisionMutateAsyncMock
            .mockRejectedValueOnce(new Error('Provisioning blew up'))
            .mockRejectedValueOnce(missingConnectionResponseError('slack'));

        renderWizard('REFERENCE');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        expect(await screen.findByText('Provisioning blew up')).toBeInTheDocument();

        clickButton('Try again');

        expect(await screen.findByText('Connect Slack to continue')).toBeInTheDocument();
        expect(screen.queryByText('Provisioning blew up')).not.toBeInTheDocument();
        expect(deprovisionMutateAsyncMock).not.toHaveBeenCalled();
    });

    it('retries after a non-409 provision failure without de-provisioning a rolled-back row', async () => {
        // `getOrCreateReference` is @Transactional(noRollbackFor = MissingConnectionException) —
        // ONLY the missing-connection case keeps its disabled row. De-provisioning after any other
        // failure throws WORKFLOW_NOT_FOUND, which would make every subsequent retry fail too.
        deprovisionMutateAsyncMock.mockRejectedValue(new Error('Workflow not found'));

        provisionMutateAsyncMock.mockRejectedValueOnce(new Error('Provisioning blew up'));

        renderWizard('REFERENCE');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        expect(await screen.findByText('Provisioning blew up')).toBeInTheDocument();

        clickButton('Try again');

        await waitFor(() => expect(provisionMutateAsyncMock).toHaveBeenCalledTimes(2));

        expect(deprovisionMutateAsyncMock).not.toHaveBeenCalled();

        // The user recovered without closing the dialog: the summary is on screen and Next is open.
        expect(await screen.findByText('My Slack')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
    });

    it('blocks activation while the wiring PUTs are still in flight', async () => {
        let resolveWiring: () => void = () => undefined;

        wireNodeConnectionMutateAsyncMock.mockImplementationOnce(
            () =>
                new Promise<void>((resolve) => {
                    resolveWiring = resolve;
                })
        );

        renderWizard('COPY');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        await waitFor(() => expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledTimes(1));

        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
        expect(screen.getByText('Connecting your accounts…')).toBeInTheDocument();

        await act(async () => {
            resolveWiring();
        });

        await waitFor(() => expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled());
    });

    it('blocks activation when a wiring PUT fails, shows a step error, and can be retried', async () => {
        wireNodeConnectionMutateAsyncMock.mockRejectedValueOnce(new Error('wiring rejected'));

        renderWizard('COPY');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        expect(await screen.findByText(WIRING_ERROR_MESSAGE)).toBeInTheDocument();

        // Publishing now would snapshot an incomplete connection list into the deployment, so the
        // activate step is unreachable.
        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'Activate'})).not.toBeInTheDocument();
        expect(publishMutateAsyncMock).not.toHaveBeenCalled();

        clickButton('Try again');

        await waitFor(() => expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledTimes(2));

        await waitFor(() => expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled());
    });

    it('drives the highlight loop when enabling reports a missing connection', async () => {
        setEnabledMutateAsyncMock.mockRejectedValueOnce(missingConnectionResponseError('slack'));

        renderWizard('REFERENCE');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        await waitFor(() => expect(provisionMutateAsyncMock).toHaveBeenCalledTimes(1));

        clickButton('Next');

        clickButton('Activate');

        expect(await screen.findByText('Connect Slack to continue')).toBeInTheDocument();

        selectConnection('Slack connection', 'My Other Slack');

        clickButton('Next');

        // The provision that succeeded DID leave a row behind, so this retry removes it first.
        await waitFor(() => expect(deprovisionMutateAsyncMock).toHaveBeenCalledWith('tpl-1'));

        expect(provisionMutateAsyncMock).toHaveBeenCalledTimes(2);
    });

    it('reuses the existing copy when an enable-time miss sends a COPY back to the connect step', async () => {
        setEnabledMutateAsyncMock.mockRejectedValueOnce(missingConnectionResponseError('slack'));

        renderWizard('COPY');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        await waitFor(() => expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled());

        clickButton('Next');

        clickButton('Activate');

        expect(await screen.findByText('Connect Slack to continue')).toBeInTheDocument();

        selectConnection('Slack connection', 'My Other Slack');

        clickButton('Next');

        await waitFor(() =>
            expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledWith({
                connectionId: 8,
                workflowConnectionKey: 'slack',
                workflowNodeName: 'sendMessage_1',
                workflowUuid: 'copy-1',
            })
        );

        // A second copyFrontendWorkflowTemplate would strand the first copy as an orphan.
        expect(copyTemplateMutateAsyncMock).toHaveBeenCalledTimes(1);
    });

    it('does not surface the SDK response-error boilerplate to the connected user', async () => {
        provisionMutateAsyncMock.mockRejectedValueOnce(
            new ResponseError(new Response('{}', {status: 500}), 'Response returned an error code')
        );

        renderWizard('REFERENCE');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
        expect(screen.queryByText('Response returned an error code')).not.toBeInTheDocument();
    });

    it('skips the connect step when no template component needs a connection', async () => {
        useGetComponentDefinitionsQueryMock.mockReturnValue({data: [], error: null, isLoading: false});

        renderWizard('COPY');

        expect(screen.queryByLabelText('Slack connection')).not.toBeInTheDocument();

        await waitFor(() => expect(copyTemplateMutateAsyncMock).toHaveBeenCalledWith('tpl-1'));

        expect(wireNodeConnectionMutateAsyncMock).not.toHaveBeenCalled();
    });

    it('wires a copied node that declares no connections under the component name as key', async () => {
        useGetWorkflowQueryMock.mockImplementation((workflowUuid?: string) => ({
            data:
                workflowUuid === 'copy-1'
                    ? {
                          definition: JSON.stringify({
                              tasks: [{name: 'sendMessage_1', type: 'slack/v1/sendMessage'}],
                          }),
                          workflowUuid,
                      }
                    : undefined,
            error: null,
            isLoading: false,
            refetch: refetchWorkflowMock,
        }));

        renderWizard('COPY');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        await waitFor(() =>
            expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledWith({
                connectionId: 7,
                workflowConnectionKey: 'slack',
                workflowNodeName: 'sendMessage_1',
                workflowUuid: 'copy-1',
            })
        );
    });
    it('does not strand the user on the connect step when Back follows a wiring failure', async () => {
        wireNodeConnectionMutateAsyncMock.mockRejectedValueOnce(new Error('wiring rejected'));

        renderWizard('COPY');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        expect(await screen.findByText(WIRING_ERROR_MESSAGE)).toBeInTheDocument();

        clickButton('Back');

        // BACK clears the error and unmounts the configure step's Try again, so the connect step's
        // Next must not be gated on wiring — otherwise this is a dead end with nothing on screen to
        // explain it.
        expect(screen.getByLabelText('Slack connection')).toBeInTheDocument();
        expect(screen.queryByText(WIRING_ERROR_MESSAGE)).not.toBeInTheDocument();

        await waitFor(() => expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled());

        // Returning with the SAME connection selected re-runs the wiring rather than
        // short-circuiting on an identical wiring key.
        clickButton('Next');

        await waitFor(() => expect(wireNodeConnectionMutateAsyncMock).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled());

        expect(copyTemplateMutateAsyncMock).toHaveBeenCalledTimes(1);
    });

    it('surfaces a failed copied-workflow load as a retryable step error instead of hanging', async () => {
        useGetWorkflowQueryMock.mockImplementation((workflowUuid?: string) => ({
            data: undefined,
            error: workflowUuid === 'copy-1' ? workflowLoadError : null,
            isLoading: false,
            refetch: refetchWorkflowMock,
        }));

        renderWizard('COPY');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        expect(await screen.findByText(WIRING_ERROR_MESSAGE)).toBeInTheDocument();

        // Without this the step sat on "Connecting your accounts…" forever: the wiring effect
        // returns early with no definition, so nothing ever failed and nothing ever completed.
        expect(screen.queryByText('Connecting your accounts…')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

        clickButton('Try again');

        await waitFor(() => expect(refetchWorkflowMock).toHaveBeenCalledTimes(1));

        expect(wireNodeConnectionMutateAsyncMock).not.toHaveBeenCalled();
    });

    it('refuses to start activation when the component-definition lookup fails', () => {
        useGetComponentDefinitionsQueryMock.mockReturnValue({
            data: undefined,
            error: new Error('definitions unavailable'),
            isLoading: false,
            refetch: refetchComponentDefinitionsMock,
        });

        renderWizard('COPY');

        expect(screen.getByText(REQUIRED_COMPONENTS_ERROR_MESSAGE)).toBeInTheDocument();

        // Collapsing the errored lookup into an empty required-component list skipped the connect
        // step, wired nothing, and published an enabled copy with no connections on it.
        expect(screen.queryByTestId('activation-wizard-stepper')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Next'})).not.toBeInTheDocument();
        expect(copyTemplateMutateAsyncMock).not.toHaveBeenCalled();
        expect(provisionMutateAsyncMock).not.toHaveBeenCalled();
        expect(publishMutateAsyncMock).not.toHaveBeenCalled();
        expect(setEnabledMutateAsyncMock).not.toHaveBeenCalled();

        clickButton('Try again');

        expect(refetchComponentDefinitionsMock).toHaveBeenCalledTimes(1);
    });

    it('leaves an open wizard alone when a background refetch fails after data already loaded', () => {
        const {rerender} = renderWizard('COPY');

        expect(screen.getByLabelText('Slack connection')).toBeInTheDocument();

        selectConnection('Slack connection', 'My Slack');

        // TanStack Query retains `data` from the last successful fetch across a failed background
        // refetch (5-minute staleTime + default refetchOnWindowFocus, or the second observer
        // HubConnectionDialog mounts on the same query key) — the wizard must not treat that as the
        // lookup having failed and unmount the reducer state underneath the user.
        useGetComponentDefinitionsQueryMock.mockReturnValue({
            data: [{icon: '<svg/>', name: 'slack', title: 'Slack'}],
            error: new Error('background refetch failed'),
            isLoading: false,
            refetch: refetchComponentDefinitionsMock,
        });

        rerender(
            <MemoryRouter>
                <ActivationWizard kind="COPY" onClose={onCloseMock} template={template} />
            </MemoryRouter>
        );

        // The wizard content (and the connection the user already picked) must still be there —
        // not replaced by the error screen.
        expect(screen.getByLabelText('Slack connection')).toBeInTheDocument();
        expect(screen.getByText('My Slack')).toBeInTheDocument();
        expect(screen.queryByTestId('activation-wizard-error')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
    });

    it('offers no Connect button when the vendor disallowed the connection dialog', () => {
        useAutomationHubStore.setState({connectionDialogAllowed: false});

        renderWizard('COPY');

        // The account select still renders — a vendor-shared connection is picked there — but the
        // affordance the vendor turned off is gone.
        expect(screen.getByLabelText('Slack connection')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add a new Slack connection'})).not.toBeInTheDocument();
    });

    it('surfaces an unreadable copied definition as a retryable error instead of a stuck step', async () => {
        useGetWorkflowQueryMock.mockImplementation((workflowUuid?: string) => ({
            data: workflowUuid === 'copy-1' ? {definition: '{not json', workflowUuid} : undefined,
            error: null,
            isLoading: false,
            refetch: refetchWorkflowMock,
        }));

        renderWizard('COPY');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        expect(await screen.findByText('The copied automation could not be read.')).toBeInTheDocument();

        // The banner used to sit above a contradictory "Connecting your accounts…" that never
        // resolved, because the parse failure left `wiringRejected` false.
        expect(screen.queryByText('Connecting your accounts…')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

        clickButton('Try again');

        await waitFor(() => expect(refetchWorkflowMock).toHaveBeenCalledTimes(1));
    });

    it('re-explains a failed definition load after Back and Next instead of a bare Try again', async () => {
        useGetWorkflowQueryMock.mockImplementation((workflowUuid?: string) => ({
            data: undefined,
            error: workflowUuid === 'copy-1' ? workflowLoadError : null,
            isLoading: false,
            refetch: refetchWorkflowMock,
        }));

        renderWizard('COPY');

        selectConnection('Slack connection', 'My Slack');

        clickButton('Next');

        expect(await screen.findByText(WIRING_ERROR_MESSAGE)).toBeInTheDocument();

        clickButton('Back');

        expect(screen.queryByText(WIRING_ERROR_MESSAGE)).not.toBeInTheDocument();

        clickButton('Next');

        // BACK clears `state.error`, so without re-arming the once-per-copy report the configure
        // step comes back carrying a Try again button and nothing that says why.
        expect(await screen.findByText(WIRING_ERROR_MESSAGE)).toBeInTheDocument();
    });
});
