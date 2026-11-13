import {ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserProjectWorkflowEditDialog from '../ConnectedUserProjectWorkflowEditDialog';

const hoisted = vi.hoisted(() => ({
    connectionsQuery: vi.fn(),
    mutate: vi.fn(),
    mutationOptions: {} as {onSuccess?: () => void},
    provisionableQuery: vi.fn(),
    referenceValuesQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useConnectedUserProjectWorkflowReferenceValuesQuery: hoisted.referenceValuesQuery,
    useProvisionConnectedUserProjectWorkflowsMutation: (options: {onSuccess?: () => void}) => {
        hoisted.mutationOptions = options;

        return {isPending: false, mutate: hoisted.mutate};
    },
    useProvisionableConnectedUserProjectsQuery: hoisted.provisionableQuery,
}));

vi.mock('@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserNewConnectionDialog', () => ({
    default: ({
        componentName,
        onConnectionCreate,
    }: {
        componentName: string;
        onConnectionCreate: (connectionId: number) => void;
    }) => (
        <button onClick={() => onConnectionCreate(99)} type="button">
            {`Create ${componentName} connection`}
        </button>
    ),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    useGetAllConnectedUserConnectionsQuery: hoisted.connectionsQuery,
}));

const SLACK_CONNECTIONS = [
    {componentName: 'slack', id: 31, name: 'Own Slack', origin: ConnectionOriginEnum.SelfServe},
    {componentName: 'slack', id: 32, name: 'Shared Slack', origin: ConnectionOriginEnum.Internal},
];

const createProjects = (workflow: object) => [
    {
        assignedConfigurationId: '60',
        configurations: [{id: '60', name: 'Slack', projectVersion: 1, selfServe: true, workflows: [workflow]}],
        name: 'Slack Project',
        projectId: '20',
        projectVersion: 1,
        workflows: [workflow],
    },
];

const DIGEST_WORKFLOW = {
    components: [{icon: null, name: 'slack', title: 'Slack'}],
    dangling: false,
    inputs: [{label: 'Channel', name: 'user.channel', required: true, type: 'string'}],
    label: 'Digest',
    provisioned: true,
    workflowUuid: 'wf-7',
};

const renderDialog = (onClose = vi.fn(), queryClient = new QueryClient()) => {
    render(
        <QueryClientProvider client={queryClient}>
            <ConnectedUserProjectWorkflowEditDialog
                configurationId="60"
                connectedUserId={7}
                connectedUserProjectWorkflowId="1052"
                onClose={onClose}
                projectId="20"
                workflowLabel="Digest"
                workflowUuid="wf-7"
            />
        </QueryClientProvider>
    );

    return {onClose, queryClient};
};

describe('ConnectedUserProjectWorkflowEditDialog', () => {
    beforeEach(() => {
        windowResizeObserver();

        Element.prototype.hasPointerCapture = vi.fn().mockReturnValue(false);
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();
        Element.prototype.setPointerCapture = vi.fn();

        hoisted.mutate.mockReset();
        hoisted.connectionsQuery.mockReturnValue({data: SLACK_CONNECTIONS});
        hoisted.provisionableQuery.mockReturnValue({
            data: {provisionableConnectedUserProjects: createProjects(DIGEST_WORKFLOW)},
        });
        hoisted.referenceValuesQuery.mockReturnValue({
            data: {
                connectedUserProjectWorkflowReferenceValues: {
                    connections: [{componentName: 'slack', connectionId: '32'}],
                    inputs: {'user.channel': '#alerts'},
                },
            },
        });
    });

    it('names the workflow in the title and reads the values of the given workflow', () => {
        renderDialog();

        expect(screen.getByText('Edit Digest Workflow')).toBeInTheDocument();
        expect(hoisted.referenceValuesQuery).toHaveBeenCalledWith({id: '1052'});
        expect(screen.getByRole('tab', {name: /Connections\s*\(1\)/})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: /Inputs\s*\(1\)/})).toBeInTheDocument();
    });

    it("preselects the workflow's current connection rather than the first one", () => {
        renderDialog();

        expect(screen.getByRole('combobox', {name: 'Slack connection'})).toHaveTextContent('Shared Slack');
    });

    it('prefills the current input values', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('tab', {name: /Inputs/}));

        expect(screen.getByLabelText('Channel *')).toHaveValue('#alerts');
    });

    it('saves the unchanged values through the provision mutation', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({
            configurationId: '60',
            connectedUserId: '7',
            workflows: [
                {
                    connections: [{componentName: 'slack', connectionId: '32'}],
                    inputs: {'user.channel': '#alerts'},
                    workflowUuid: 'wf-7',
                },
            ],
        });
    });

    it('saves a changed connection and a changed input', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('combobox', {name: 'Slack connection'}));
        await userEvent.click(screen.getByRole('option', {name: /Own Slack/}));

        await userEvent.click(screen.getByRole('tab', {name: /Inputs/}));

        const channelInput = screen.getByLabelText('Channel *');

        await userEvent.clear(channelInput);
        await userEvent.type(channelInput, '#general');

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({
            configurationId: '60',
            connectedUserId: '7',
            workflows: [
                {
                    connections: [{componentName: 'slack', connectionId: '31'}],
                    inputs: {'user.channel': '#general'},
                    workflowUuid: 'wf-7',
                },
            ],
        });
    });

    it('selects a connection created from the dialog', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('button', {name: 'New Slack connection'}));
        await userEvent.click(screen.getByRole('button', {name: 'Create slack connection'}));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.mutate.mock.calls[0][0].workflows[0].connections).toEqual([
            {componentName: 'slack', connectionId: '99'},
        ]);
    });

    it('keeps Save disabled while a required input is empty', async () => {
        renderDialog();

        await userEvent.click(screen.getByRole('tab', {name: /Inputs/}));
        await userEvent.clear(screen.getByLabelText('Channel *'));

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it('explains a workflow whose configuration is not released and keeps Save disabled', () => {
        hoisted.provisionableQuery.mockReturnValue({data: {provisionableConnectedUserProjects: []}});

        renderDialog();

        expect(screen.getByText(/its configuration is not released/)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it('keeps Save disabled for a workflow that needs attention', () => {
        hoisted.provisionableQuery.mockReturnValue({
            data: {provisionableConnectedUserProjects: createProjects({...DIGEST_WORKFLOW, dangling: true})},
        });

        renderDialog();

        expect(screen.getByText(/needs attention/)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it('keeps Save disabled when the configuration provides every connection and input', () => {
        hoisted.provisionableQuery.mockReturnValue({
            data: {
                provisionableConnectedUserProjects: createProjects({...DIGEST_WORKFLOW, components: [], inputs: []}),
            },
        });

        renderDialog();

        expect(screen.getByText(/No connections to set/)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
    });

    it('refreshes the workflow list and closes after a save', () => {
        const queryClient = new QueryClient();

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        const {onClose} = renderDialog(vi.fn(), queryClient);

        hoisted.mutationOptions.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUserProjects']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({
            queryKey: ['connectedUserProjectWorkflowReferenceValues'],
        });
        expect(onClose).toHaveBeenCalled();
    });
});
