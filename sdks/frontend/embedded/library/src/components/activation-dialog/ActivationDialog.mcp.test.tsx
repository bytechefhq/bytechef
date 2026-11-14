import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ActivationDialog from './ActivationDialog';

vi.mock('../connect-dialog/index', () => ({
    default: () => ({closeDialog: vi.fn(), openDialog: vi.fn()}),
}));

type FetchCall = {method: string; url: string};

const calls: FetchCall[] = [];

let userWorkflows: Array<Record<string, unknown>> = [];
let userWorkflowsAfterProvision: Array<Record<string, unknown>> | undefined;

const respond = (status: number, body: unknown) => ({
    headers: {get: () => null},
    json: () => Promise.resolve(body),
    ok: status < 400,
    status,
});

const installFetch = () => {
    global.fetch = vi.fn(async (url: string, init?: RequestInit) => {
        const method = init?.method ?? 'GET';

        calls.push({method, url});

        if (method === 'GET' && /\/automation\/workflow-templates\/wf-plain$/.test(url)) {
            return respond(200, {components: [], id: 'wf-plain', inputs: [], label: 'Plain'});
        }

        if (method === 'GET' && url.endsWith('/automation/workflows')) {
            return respond(200, userWorkflows);
        }

        if (method === 'POST' && url.endsWith('/provision') && userWorkflowsAfterProvision) {
            userWorkflows = userWorkflowsAfterProvision;
        }

        if (url.endsWith('/mcp/enable')) {
            userWorkflows = userWorkflows.map((workflow) => ({...workflow, mcpEnabled: method === 'POST'}));
        }

        return respond(204, undefined);
    }) as unknown as typeof fetch;
};

const renderDialog = () =>
    render(
        <ActivationDialog
            baseUrl="https://api.example.com"
            environment="PRODUCTION"
            jwtToken="ey"
            onClose={vi.fn()}
            workflowTemplateId="wf-plain"
        />
    );

const mcpCalls = () => calls.filter((call) => call.url.endsWith('/mcp/enable'));

describe('ActivationDialog Available to AI agents switch', () => {
    beforeEach(() => {
        calls.length = 0;
        userWorkflows = [];
        userWorkflowsAfterProvision = undefined;
        installFetch();
        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('showsAiAgentsSwitchWhenOffered', async () => {
        userWorkflows = [{automationUuid: 'wf-plain', kind: 'REFERENCE', mcpEnabled: false}];

        renderDialog();

        expect(await screen.findByRole('switch', {name: 'Available to AI agents'})).not.toBeChecked();
    });

    it('hidesAiAgentsSwitchWhenNotOffered', async () => {
        userWorkflows = [{automationUuid: 'wf-plain', kind: 'REFERENCE', mcpEnabled: null}];

        renderDialog();

        expect(await screen.findByRole('button', {name: 'Save'})).toBeInTheDocument();
        expect(screen.queryByRole('switch', {name: 'Available to AI agents'})).not.toBeInTheDocument();
    });

    it('togglingAiAgentsSwitchCallsMcpEnable', async () => {
        userWorkflows = [{automationUuid: 'wf-plain', kind: 'REFERENCE', mcpEnabled: false}];

        renderDialog();

        const aiAgentsSwitch = await screen.findByRole('switch', {name: 'Available to AI agents'});

        await userEvent.click(aiAgentsSwitch);

        await waitFor(() => {
            expect(mcpCalls()).toEqual([
                {
                    method: 'POST',
                    url: 'https://api.example.com/api/embedded/v1/automation/workflows/wf-plain/mcp/enable',
                },
            ]);
        });

        await waitFor(() => expect(aiAgentsSwitch).toBeChecked());

        await userEvent.click(aiAgentsSwitch);

        await waitFor(() => {
            expect(mcpCalls().map((call) => call.method)).toEqual(['POST', 'DELETE']);
        });

        await waitFor(() => expect(aiAgentsSwitch).not.toBeChecked());

        expect(calls.some((call) => call.url.endsWith('/wf-plain/enable'))).toBe(false);
    });

    it('toolOnlyReferenceSavesWithoutOwnEnable', async () => {
        const onClose = vi.fn();

        userWorkflows = [{automationUuid: 'wf-plain', kind: 'REFERENCE', mcpEnabled: true, toolOnly: true}];

        render(
            <ActivationDialog
                baseUrl="https://api.example.com"
                environment="PRODUCTION"
                jwtToken="ey"
                onClose={onClose}
                workflowTemplateId="wf-plain"
            />
        );

        expect(await screen.findByRole('switch', {name: 'Available to AI agents'})).toBeChecked();

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(onClose).toHaveBeenCalled());

        expect(calls.some((call) => call.url.endsWith('/wf-plain/enable'))).toBe(false);
    });

    it('showsAiAgentsSwitchRightAfterFirstActivation', async () => {
        userWorkflowsAfterProvision = [{automationUuid: 'wf-plain', kind: 'REFERENCE', mcpEnabled: true}];

        renderDialog();

        await userEvent.click(await screen.findByRole('button', {name: 'Activate'}));

        expect(await screen.findByRole('switch', {name: 'Available to AI agents'})).toBeChecked();
    });

    it('unactivatedToolOnlyTemplateStillShowsActivationFlow', async () => {
        renderDialog();

        expect(await screen.findByRole('button', {name: 'Activate'})).toBeInTheDocument();
        expect(screen.queryByRole('switch', {name: 'Available to AI agents'})).not.toBeInTheDocument();
    });
});
