import {describe, it, expect, vi, beforeEach, afterEach} from 'vitest';
import {act, render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ActivationDialog from './ActivationDialog';

const {useConnectDialogMock, openConnectDialogMock} = vi.hoisted(() => ({
    openConnectDialogMock: vi.fn(),
    useConnectDialogMock: vi.fn(),
}));

vi.mock('../connect-dialog/index', () => ({
    default: useConnectDialogMock,
}));

const projects = [
    {
        id: 1,
        name: 'Sales',
        workflowTemplates: [
            {
                components: [
                    {connectionRequired: true, integrationId: 5, name: 'slack', title: 'Slack'},
                    {connectionRequired: false, name: 'logger', title: 'Logger'},
                ],
                id: 'wf-slack',
                inputs: [{label: 'Channel', name: 'channel', required: true}],
                label: 'Daily standup reminder',
            },
            {
                components: [{connectionRequired: true, integrationId: null, name: 'gmail', title: 'Gmail'}],
                id: 'wf-gmail',
                inputs: [],
                label: 'Mail digest',
            },
            {components: [], id: 'wf-plain', inputs: [], label: 'Plain'},
        ],
    },
    {
        id: 2,
        kind: 'REFERENCE',
        name: 'Code workflows',
        workflowTemplates: [{components: [], id: 'wf-code', inputs: [], label: 'Code workflow'}],
    },
];

type FetchCall = {body?: string; method: string; url: string};

const calls: FetchCall[] = [];

const respond = (status: number, body: unknown) => ({
    headers: {get: () => null},
    json: () => Promise.resolve(body),
    ok: status < 400,
    status,
});

let connections: Array<{componentName: string; id: number; name: string}> = [];
let connectionsStatus = 200;
let deprovisionStatus = 200;
let provisionGate: Promise<void> | undefined;
let enableResponse: {body: unknown; status: number} = {body: {}, status: 200};
let provisionResponse: {body: unknown; status: number} = {body: {}, status: 200};
let workflowsResponse: {body: unknown; status: number} = {body: [], status: 200};
let copyResponse: {body: unknown; status: number} = {body: 'wf-copy', status: 200};

const installFetch = () => {
    global.fetch = vi.fn(async (url: string, init?: RequestInit) => {
        const method = init?.method ?? 'GET';

        calls.push({body: init?.body as string | undefined, method, url});

        if (url.endsWith('/automation/projects')) {
            return respond(200, projects);
        }

        if (url.includes('/components/slack/connections')) {
            return respond(connectionsStatus, connections);
        }

        if (url.endsWith('/automation/workflows') && method === 'GET') {
            return respond(workflowsResponse.status, workflowsResponse.body);
        }

        if (url.endsWith('/provision') && method === 'POST') {
            await provisionGate;

            return respond(provisionResponse.status, provisionResponse.body);
        }

        if (url.endsWith('/provision') && method === 'DELETE') {
            return respond(deprovisionStatus, {});
        }

        if (url.endsWith('/enable') && method === 'POST') {
            return respond(enableResponse.status, enableResponse.body);
        }

        if (url.endsWith('/copy') && method === 'POST') {
            return respond(copyResponse.status, copyResponse.body);
        }

        return respond(200, {});
    }) as unknown as typeof fetch;
};

const renderDialog = (overrides: Partial<React.ComponentProps<typeof ActivationDialog>> = {}) =>
    render(
        <ActivationDialog
            baseUrl="https://api.example.com"
            environment="PRODUCTION"
            jwtToken="ey"
            onClose={vi.fn()}
            workflowTemplateId="wf-slack"
            {...overrides}
        />
    );

describe('ActivationDialog', () => {
    beforeEach(() => {
        calls.length = 0;
        connections = [];
        connectionsStatus = 200;
        deprovisionStatus = 200;
        provisionGate = undefined;
        enableResponse = {body: {}, status: 200};
        provisionResponse = {body: {}, status: 200};
        workflowsResponse = {body: [], status: 200};
        copyResponse = {body: 'wf-copy', status: 200};
        installFetch();
        useConnectDialogMock.mockReturnValue({closeDialog: vi.fn(), openDialog: openConnectDialogMock});
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
        useConnectDialogMock.mockReset();
        openConnectDialogMock.mockReset();
    });

    it('shows an error state when the template id is unknown', async () => {
        renderDialog({workflowTemplateId: 'nope'});

        expect(await screen.findByText(/could not be found/i)).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Next'})).not.toBeInTheDocument();
    });

    it('lists only connection-requiring components and gates Next on a selection', async () => {
        connections = [{componentName: 'slack', id: 9, name: 'Team Slack'}];

        renderDialog();

        const row = await screen.findByTestId('connect-row-slack');

        expect(screen.queryByTestId('connect-row-logger')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();

        await within(row).findByRole('option', {name: 'Team Slack'});

        await userEvent.selectOptions(within(row).getByRole('combobox'), '9');

        expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
    });

    it('opens ConnectDialog for the component integration and selects the connection it reports', async () => {
        renderDialog();

        const row = await screen.findByTestId('connect-row-slack');

        connections = [{componentName: 'slack', id: 11, name: 'New Slack'}];

        await userEvent.click(within(row).getByRole('button', {name: 'Connect'}));

        await waitFor(() => expect(openConnectDialogMock).toHaveBeenCalled());

        const connectProps = useConnectDialogMock.mock.calls[useConnectDialogMock.mock.calls.length - 1][0];

        expect(connectProps.integrationId).toBe('5');
        expect(connectProps.connectionOnly).toBe(true);

        await act(async () => connectProps.onConnected(42, 11));

        await waitFor(() => expect(within(row).getByRole('combobox')).toHaveValue('11'));
        expect(screen.getByRole('button', {name: 'Next'})).toBeEnabled();
    });

    it('tells the user when the component integration is not set up and blocks Next', async () => {
        renderDialog({workflowTemplateId: 'wf-gmail'});

        expect(await screen.findByText(/Gmail isn't set up yet/)).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Connect'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
    });

    it('hides Connect when connection creation is not allowed', async () => {
        renderDialog({connectionCreationAllowed: false});

        await screen.findByTestId('connect-row-slack');

        expect(screen.queryByRole('button', {name: 'Connect'})).not.toBeInTheDocument();
    });

    it('provisions with the selected connections and inputs, enables, and reports the uuid', async () => {
        connections = [{componentName: 'slack', id: 9, name: 'Team Slack'}];

        const onActivated = vi.fn();

        renderDialog({onActivated});

        const row = await screen.findByTestId('connect-row-slack');

        await within(row).findByRole('option', {name: 'Team Slack'});

        await userEvent.selectOptions(within(row).getByRole('combobox'), '9');
        await userEvent.click(screen.getByRole('button', {name: 'Next'}));
        await userEvent.type(screen.getByLabelText(/Channel/), '#general');
        await userEvent.click(screen.getByRole('button', {name: 'Next'}));
        await userEvent.click(screen.getByRole('button', {name: 'Activate'}));

        await waitFor(() => expect(onActivated).toHaveBeenCalledWith('wf-slack'));

        const provision = calls.find(
            (call) => call.url.endsWith('/workflow-templates/wf-slack/provision') && call.method === 'POST'
        );
        const enable = calls.find((call) => call.url.endsWith('/workflows/wf-slack/enable') && call.method === 'POST');

        expect(JSON.parse(provision!.body!)).toEqual({connections: {slack: 9}, inputs: {channel: '#general'}});
        expect(enable!.body).toBeUndefined();
        expect(calls.indexOf(provision!)).toBeLessThan(calls.indexOf(enable!));
        expect(screen.getByRole('button', {name: 'Done'})).toBeInTheDocument();
    });

    it('returns to Connect and highlights the component the server reports missing, after deprovisioning', async () => {
        connections = [{componentName: 'slack', id: 9, name: 'Team Slack'}];
        provisionResponse = {body: {missingConnectionComponentName: 'slack'}, status: 409};

        renderDialog({workflowTemplateId: 'wf-slack'});

        const row = await screen.findByTestId('connect-row-slack');

        await within(row).findByRole('option', {name: 'Team Slack'});

        await userEvent.selectOptions(within(row).getByRole('combobox'), '9');
        await userEvent.click(screen.getByRole('button', {name: 'Next'}));
        await userEvent.type(screen.getByLabelText(/Channel/), '#general');
        await userEvent.click(screen.getByRole('button', {name: 'Next'}));
        await userEvent.click(screen.getByRole('button', {name: 'Activate'}));

        expect(await screen.findByText(/Connect Slack to continue/)).toBeInTheDocument();
        expect(
            calls.some(
                (call) => call.url.endsWith('/workflow-templates/wf-slack/provision') && call.method === 'DELETE'
            )
        ).toBe(true);
        expect(screen.getByRole('button', {name: 'Next'})).toBeDisabled();
    });

    const isDeprovision = (call: FetchCall) =>
        call.url.endsWith('/workflow-templates/wf-plain/provision') && call.method === 'DELETE';

    it('keeps an automation that was already activated when re-activating it fails', async () => {
        provisionResponse = {body: {missingConnectionComponentName: 'slack'}, status: 409};
        workflowsResponse = {
            body: [
                {automationWorkflowUuid: 'wf-other', kind: 'REFERENCE', name: 'Other', tasks: []},
                {automationWorkflowUuid: 'wf-plain', kind: 'REFERENCE', name: 'Plain', tasks: []},
            ],
            status: 200,
        };

        renderDialog({workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Activate'}));

        expect(await screen.findByRole('alert')).toBeInTheDocument();
        expect(calls.some(isDeprovision)).toBe(false);
    });

    it('removes a first activation that failed to enable', async () => {
        enableResponse = {body: {}, status: 500};
        workflowsResponse = {
            body: [{copiedFromWorkflowUuid: 'wf-plain', kind: 'COPY', name: 'Plain copy', tasks: []}],
            status: 200,
        };

        renderDialog({workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Activate'}));

        expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
        await waitFor(() => expect(calls.some(isDeprovision)).toBe(true));
    });

    it('removes a first activation when only another template was activated before', async () => {
        enableResponse = {body: {}, status: 500};
        workflowsResponse = {
            body: [{automationWorkflowUuid: 'wf-other', kind: 'REFERENCE', name: 'Other', tasks: []}],
            status: 200,
        };

        renderDialog({workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Activate'}));

        await waitFor(() => expect(calls.some(isDeprovision)).toBe(true));
    });

    it('does not provision when it cannot tell whether the automation existed before', async () => {
        workflowsResponse = {body: {}, status: 500};

        renderDialog({workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Activate'}));

        expect(await screen.findByText('Could not check your automations. Please try again.')).toBeInTheDocument();
        expect(calls.some((call) => call.url.endsWith('/provision'))).toBe(false);
    });

    it('says so when a failed first activation could not be undone', async () => {
        enableResponse = {body: {}, status: 500};
        deprovisionStatus = 500;

        renderDialog({workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Activate'}));

        expect(await screen.findByText(/could not be fully undone/)).toBeInTheDocument();
    });

    it('says the settings were saved when re-activating an existing automation fails to enable', async () => {
        enableResponse = {body: {}, status: 500};
        workflowsResponse = {
            body: [{automationWorkflowUuid: 'wf-plain', kind: 'REFERENCE', name: 'Plain', tasks: []}],
            status: 200,
        };

        renderDialog({workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Activate'}));

        expect(
            await screen.findByText(/settings were saved, but the automation could not be switched on/)
        ).toBeInTheDocument();
        expect(calls.some(isDeprovision)).toBe(false);
    });

    it('keeps the automation on when the host onActivated callback throws', async () => {
        const onActivated = vi.fn(() => {
            throw new Error('host bug');
        });

        renderDialog({onActivated, workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Activate'}));

        expect(await screen.findByRole('button', {name: 'Done'})).toBeInTheDocument();
        expect(onActivated).toHaveBeenCalledWith('wf-plain');
        expect(calls.some(isDeprovision)).toBe(false);
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('refuses a second Activate click while activating', async () => {
        let releaseProvision: () => void = () => {};

        provisionGate = new Promise((resolve) => {
            releaseProvision = resolve;
        });

        renderDialog({workflowTemplateId: 'wf-plain'});

        const activateButton = await screen.findByRole('button', {name: 'Activate'});

        await userEvent.click(activateButton);

        await waitFor(() => expect(activateButton).toBeDisabled());

        await userEvent.click(activateButton);

        await act(async () => releaseProvision());

        expect(await screen.findByRole('button', {name: 'Done'})).toBeInTheDocument();
        expect(calls.filter((call) => call.url.endsWith('/provision') && call.method === 'POST')).toHaveLength(1);
    });

    it('asks the user to reload when the session has expired', async () => {
        provisionResponse = {body: {}, status: 401};

        renderDialog({workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Activate'}));

        expect(await screen.findByText(/session has expired/)).toBeInTheDocument();
    });

    it('shows when the accounts could not be loaded and loads them again on Retry', async () => {
        connectionsStatus = 500;

        renderDialog();

        const row = await screen.findByTestId('connect-row-slack');

        expect(await within(row).findByText("Couldn't load your Slack accounts.")).toBeInTheDocument();

        connectionsStatus = 200;
        connections = [{componentName: 'slack', id: 9, name: 'Team Slack'}];

        await userEvent.click(within(row).getByRole('button', {name: 'Retry'}));

        expect(await within(row).findByRole('option', {name: 'Team Slack'})).toBeInTheDocument();
        expect(within(row).queryByText("Couldn't load your Slack accounts.")).not.toBeInTheDocument();
    });

    it('is a labelled modal dialog', async () => {
        renderDialog({workflowTemplateId: 'wf-plain'});

        await screen.findByRole('button', {name: 'Activate'});

        const dialog = screen.getByRole('dialog', {name: 'Plain'});

        expect(dialog).toHaveAttribute('aria-modal', 'true');
    });

    it('shows a generic error on any other failure', async () => {
        provisionResponse = {body: {}, status: 500};

        renderDialog({workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Activate'}));

        expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
    });

    it('does not offer Edit workflow for a template from a code-workflow (REFERENCE) project', async () => {
        renderDialog({onEditWorkflow: vi.fn(), workflowTemplateId: 'wf-code'});

        expect(await screen.findByRole('button', {name: 'Activate'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Edit workflow'})).not.toBeInTheDocument();
    });

    it('offers Edit workflow for a template from a project without a kind', async () => {
        renderDialog({onEditWorkflow: vi.fn(), workflowTemplateId: 'wf-plain'});

        expect(await screen.findByRole('button', {name: 'Edit workflow'})).toBeInTheDocument();
    });

    it('offers Edit workflow only when a handler is given, copies the template and hands over the uuid', async () => {
        renderDialog({workflowTemplateId: 'wf-plain'});

        await screen.findByRole('button', {name: 'Activate'});

        expect(screen.queryByRole('button', {name: 'Edit workflow'})).not.toBeInTheDocument();

        const onEditWorkflow = vi.fn();
        const onClose = vi.fn();

        renderDialog({onClose, onEditWorkflow, workflowTemplateId: 'wf-plain'});

        await userEvent.click((await screen.findAllByRole('button', {name: 'Edit workflow'}))[0]);

        await waitFor(() => expect(onEditWorkflow).toHaveBeenCalledWith('wf-copy'));
        expect(onClose).toHaveBeenCalled();
    });

    const alreadyCopiedBody = {
        detail: 'Automation workflow template wf-plain is already copied',
        entityClass: 'ConnectedUserProjectWorkflow',
        errorKey: 100,
        inputParameters: {copiedWorkflowUuid: 'wf-existing'},
        status: 400,
    };

    it('opens the existing copy when the template was already copied', async () => {
        copyResponse = {body: alreadyCopiedBody, status: 400};

        const onEditWorkflow = vi.fn();
        const onClose = vi.fn();

        renderDialog({onClose, onEditWorkflow, workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Edit workflow'}));

        await waitFor(() => expect(onEditWorkflow).toHaveBeenCalledWith('wf-existing'));
        expect(onClose).toHaveBeenCalled();
        expect(screen.queryByText('Something went wrong. Please try again.')).not.toBeInTheDocument();
    });

    it('shows the generic error for a different 400 on copy', async () => {
        copyResponse = {body: {...alreadyCopiedBody, errorKey: 101}, status: 400};

        const onEditWorkflow = vi.fn();

        renderDialog({onEditWorkflow, workflowTemplateId: 'wf-plain'});

        await userEvent.click(await screen.findByRole('button', {name: 'Edit workflow'}));

        expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
        expect(onEditWorkflow).not.toHaveBeenCalled();
    });

    it('closes on Escape, but not while the connect form is open', async () => {
        const onClose = vi.fn();

        renderDialog({onClose});

        const row = await screen.findByTestId('connect-row-slack');

        await userEvent.click(within(row).getByRole('button', {name: 'Connect'}));
        await waitFor(() => expect(openConnectDialogMock).toHaveBeenCalled());

        await userEvent.keyboard('{Escape}');

        expect(onClose).not.toHaveBeenCalled();

        const connectProps = useConnectDialogMock.mock.calls[useConnectDialogMock.mock.calls.length - 1][0];

        await act(async () => connectProps.onClose());

        await userEvent.keyboard('{Escape}');

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it.each([
        ['after', false],
        ['before', true],
    ])('stays open on Escape when the connect dialog listener runs %s it', async (_order, capture) => {
        const onClose = vi.fn();
        const {rerender} = renderDialog({onClose});

        const row = await screen.findByTestId('connect-row-slack');

        await userEvent.click(within(row).getByRole('button', {name: 'Connect'}));
        await waitFor(() => expect(openConnectDialogMock).toHaveBeenCalled());

        const connectProps = useConnectDialogMock.mock.calls[useConnectDialogMock.mock.calls.length - 1][0];
        const simulateConnectDialogEscape = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                connectProps.onClose();
            }
        };

        window.addEventListener('keydown', simulateConnectDialogEscape, {capture});

        const inlineOnClose = vi.fn();

        rerender(
            <ActivationDialog
                baseUrl="https://api.example.com"
                environment="PRODUCTION"
                jwtToken="ey"
                onClose={inlineOnClose}
                workflowTemplateId="wf-slack"
            />
        );

        await userEvent.keyboard('{Escape}');

        window.removeEventListener('keydown', simulateConnectDialogEscape, {capture});

        expect(onClose).not.toHaveBeenCalled();
        expect(inlineOnClose).not.toHaveBeenCalled();

        await act(async () => {
            await new Promise((resolve) => setTimeout(resolve, 0));
        });
    });
});
