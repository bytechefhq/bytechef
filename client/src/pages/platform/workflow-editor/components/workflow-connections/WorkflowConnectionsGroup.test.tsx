import {TooltipProvider} from '@/components/ui/tooltip';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {
    mockScrollIntoView,
    render,
    screen,
    userEvent,
    waitFor,
    windowResizeObserver,
    within,
} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowConnectionsGroup, {WorkflowConnectionsGroupProps} from './WorkflowConnectionsGroup';
import {WorkflowConnectionGroupI, buildWorkflowConnectionGroups} from './utils/workflowConnectionGroups';

const hoisted = vi.hoisted(() => ({
    connections: [] as Array<Record<string, unknown>>,
    useGetComponentDefinitionQueryMock: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
        useCreateConnectionMutation: () => ({mutate: vi.fn()}),
        useGetComponentDefinitionsQuery: () => ({data: []}),
        useGetConnectionTagsQuery: () => ({data: []}),
        useGetConnectionsQuery: () => ({data: hoisted.connections}),
    }),
}));

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: () => ({data: {componentName: 'googleSheets', version: 1}}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: (request: unknown, enabled?: boolean) =>
        hoisted.useGetComponentDefinitionQueryMock(request, enabled),
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: ({onConnectionCreate}: {onConnectionCreate: (connectionId: number) => void}) => (
        <div aria-label="New connection" role="dialog">
            <button onClick={() => onConnectionCreate(42)}>Save connection</button>
        </div>
    ),
}));

const GOOGLE_SHEETS_CONNECTIONS = [
    {componentName: 'googleSheets', connectionVersion: 1, id: 7, name: 'Finance Sheets', parameters: {}},
    {componentName: 'googleSheets', connectionVersion: 1, id: 9, name: 'Ops Sheets', parameters: {}},
    {componentName: 'googleSheets', connectionVersion: 1, id: 12, name: 'Marketing Sheets', parameters: {}},
];

const onChangesRequest = vi.fn();

let queryClient: QueryClient;

function createGroup(connectionIds: Array<number | undefined>): WorkflowConnectionGroupI {
    const tasks = connectionIds.map((connectionId, index) => ({connectionId, taskName: `googleSheets_${index + 1}`}));

    const workflow = {
        tasks: tasks.map(({taskName}) => ({
            connections: [
                {
                    componentName: 'googleSheets',
                    componentVersion: 1,
                    key: 'googleSheets',
                    required: true,
                    workflowNodeName: taskName,
                },
            ],
            label: 'Insert Row',
            name: taskName,
            type: 'googleSheets/v1/insertRow',
        })),
        triggers: [],
    } as unknown as Workflow;

    const testConfigurationConnections = tasks.flatMap(({connectionId, taskName}) =>
        connectionId === undefined
            ? []
            : [{connectionId, workflowConnectionKey: 'googleSheets', workflowNodeName: taskName}]
    );

    return buildWorkflowConnectionGroups(workflow, testConfigurationConnections)[0];
}

function renderGroup(props: Partial<WorkflowConnectionsGroupProps> & {group: WorkflowConnectionGroupI}) {
    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>{children}</TooltipProvider>
        </QueryClientProvider>
    );

    return render(
        <WorkflowConnectionsGroup
            componentDefinition={{name: 'googleSheets', title: 'Google Sheets', version: 1}}
            disabled={false}
            onChangesRequest={onChangesRequest}
            pending={false}
            title="Google Sheets"
            {...props}
        />,
        {wrapper}
    );
}

function getGroupSelect() {
    return screen.getByRole('combobox', {name: 'Google Sheets connection'});
}

async function pickOption(combobox: HTMLElement, optionName: string) {
    await userEvent.click(combobox);
    await userEvent.click(await screen.findByRole('option', {name: optionName}));
}

beforeEach(() => {
    mockScrollIntoView();
    windowResizeObserver();

    Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();
    Element.prototype.releasePointerCapture = Element.prototype.releasePointerCapture || vi.fn();

    queryClient = new QueryClient();

    vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue(undefined);

    hoisted.connections = GOOGLE_SHEETS_CONNECTIONS;
    hoisted.useGetComponentDefinitionQueryMock.mockImplementation((request: unknown, enabled?: boolean) => ({
        data: enabled ? {name: 'googleSheets', title: 'Google Sheets', version: 1} : undefined,
    }));

    useWorkflowNodeDetailsPanelStore.setState({connectionDialogAllowed: true});
});

afterEach(() => {
    vi.clearAllMocks();
});

describe('WorkflowConnectionsGroup', () => {
    it('renders the title, task count, override count and status', () => {
        renderGroup({group: createGroup([7, 7, 7, 7, 9])});

        expect(screen.getByRole('group', {name: 'Google Sheets'})).toBeInTheDocument();
        expect(screen.getByText('Google Sheets', {selector: 'span'})).toBeInTheDocument();
        expect(screen.getByText('5 tasks, 1 override')).toBeInTheDocument();
        expect(screen.getByText('5/5')).toBeInTheDocument();
    });

    it('keeps the card header out of the banner landmarks', () => {
        renderGroup({group: createGroup([7, 9, 9])});

        expect(
            within(screen.getByRole('group', {name: 'Google Sheets'})).queryByRole('banner')
        ).not.toBeInTheDocument();
    });

    it('shows a single task without an override count or a task list', () => {
        renderGroup({group: createGroup([undefined])});

        expect(screen.getByText('1 task')).toBeInTheDocument();
        expect(screen.getByText('0/1')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Show tasks'})).not.toBeInTheDocument();
    });

    it('names the task of a single-task group', () => {
        renderGroup({group: createGroup([7])});

        expect(screen.getByText('googleSheets_1').closest('p')).toHaveTextContent('googleSheets_1 Insert Row');
    });

    it('names the cluster element of a single-element group with its root', () => {
        const workflow = {
            tasks: [
                {
                    clusterRoot: true,
                    connections: [
                        {
                            componentName: 'openAi',
                            componentVersion: 1,
                            key: 'openAi_1',
                            required: true,
                            workflowNodeName: 'aiAgent_1',
                        },
                    ],
                    name: 'aiAgent_1',
                    type: 'aiAgent/v1/chat',
                },
            ],
            triggers: [],
        } as unknown as Workflow;

        renderGroup({group: buildWorkflowConnectionGroups(workflow)[0], title: 'OpenAI'});

        expect(screen.getByText('openAi_1').closest('p')).toHaveTextContent('openAi_1 in aiAgent_1');
    });

    it('leaves task names to the rows when a group has several tasks', () => {
        renderGroup({group: createGroup([7, 7])});

        expect(screen.queryByText('googleSheets_1')).not.toBeInTheDocument();
    });

    it('shows the group connection in the select', () => {
        renderGroup({group: createGroup([7, 7, 9])});

        expect(getGroupSelect()).toHaveTextContent('Finance Sheets');
    });

    it('shows Mixed: 2 connections on a tie', () => {
        renderGroup({group: createGroup([7, 9])});

        expect(getGroupSelect()).toHaveTextContent('Mixed: 2 connections');
    });

    it('asks for a connection when no task has one', () => {
        renderGroup({group: createGroup([undefined, undefined])});

        expect(getGroupSelect()).toHaveTextContent('Choose Connection...');
    });

    it("clears a single task's connection from the group select", async () => {
        renderGroup({group: createGroup([7])});

        await pickOption(getGroupSelect(), 'Select...');

        expect(onChangesRequest).toHaveBeenCalledTimes(1);
        expect(onChangesRequest).toHaveBeenCalledWith([
            {
                connectionId: undefined,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_1',
            },
        ]);
    });

    it('offers Select... in the group select only for a single task with a connection', async () => {
        const {unmount} = renderGroup({group: createGroup([undefined])});

        await userEvent.click(getGroupSelect());
        await screen.findByRole('option', {name: 'Finance Sheets'});

        expect(screen.queryByRole('option', {name: 'Select...'})).not.toBeInTheDocument();

        unmount();

        renderGroup({group: createGroup([7, 7])});

        await userEvent.click(getGroupSelect());
        await screen.findByRole('option', {name: 'Finance Sheets'});

        expect(screen.queryByRole('option', {name: 'Select...'})).not.toBeInTheDocument();
    });

    it('requests changes for the group when a connection is picked', async () => {
        renderGroup({group: createGroup([7, 7, undefined, 9])});

        await pickOption(getGroupSelect(), 'Marketing Sheets');

        expect(onChangesRequest).toHaveBeenCalledTimes(1);
        expect(onChangesRequest).toHaveBeenCalledWith([
            {
                connectionId: 12,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_1',
            },
            {
                connectionId: 12,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
            {
                connectionId: 12,
                previousConnectionId: undefined,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_3',
            },
        ]);
    });

    it('changes both tasks of a tie when a connection is picked', async () => {
        renderGroup({group: createGroup([7, 9])});

        await pickOption(getGroupSelect(), 'Finance Sheets');

        expect(onChangesRequest).toHaveBeenCalledWith([
            {
                connectionId: 7,
                previousConnectionId: 9,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
        ]);
    });

    it('offers Use for all Google Sheets tasks when the group connection is set and two tasks are empty', async () => {
        renderGroup({group: createGroup([7, undefined, undefined])});

        await userEvent.click(screen.getByRole('button', {name: 'Use for all Google Sheets tasks'}));

        expect(onChangesRequest).toHaveBeenCalledWith([
            {
                connectionId: 7,
                previousConnectionId: undefined,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
            {
                connectionId: 7,
                previousConnectionId: undefined,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_3',
            },
        ]);
    });

    it('offers the fill action for a single empty task and hides it when nothing is empty', () => {
        const {unmount} = renderGroup({group: createGroup([7, 7, undefined])});

        expect(screen.getByRole('button', {name: 'Use for all Google Sheets tasks'})).toBeInTheDocument();

        unmount();

        renderGroup({group: createGroup([7, 7])});

        expect(screen.queryByRole('button', {name: /^Use for/})).not.toBeInTheDocument();
    });

    it('shows the mixed hint and the overrides hint', () => {
        const {unmount} = renderGroup({group: createGroup([7, 9])});

        expect(screen.getByText('Choosing a connection applies it to all 2 tasks.')).toBeInTheDocument();
        expect(screen.queryByText('Tasks marked Override keep their own connection.')).not.toBeInTheDocument();

        unmount();

        renderGroup({group: createGroup([7, 7, 9])});

        expect(screen.getByText('Tasks marked Override keep their own connection.')).toBeInTheDocument();
        expect(screen.queryByText(/^Choosing a connection applies it/)).not.toBeInTheDocument();
    });

    it('lists tasks when expanded, marks overrides and resets one to the group connection', async () => {
        renderGroup({group: createGroup([7, 7, 9])});

        expect(screen.queryByRole('listitem')).not.toBeInTheDocument();

        const expandButton = screen.getByRole('button', {name: 'Show tasks'});

        expect(expandButton).toHaveAttribute('aria-expanded', 'false');

        await userEvent.click(expandButton);

        expect(screen.getByRole('button', {name: 'Hide tasks'})).toHaveAttribute('aria-expanded', 'true');

        const rows = screen.getAllByRole('listitem');

        expect(rows).toHaveLength(3);
        expect(within(rows[0]).getByText('googleSheets_1')).toBeInTheDocument();
        expect(within(rows[0]).getByText('Insert Row')).toBeInTheDocument();
        expect(within(rows[0]).queryByText('Override')).not.toBeInTheDocument();
        expect(within(rows[1]).queryByText('Override')).not.toBeInTheDocument();
        expect(within(rows[2]).getByText('Override')).toBeInTheDocument();
        expect(within(rows[2]).getByRole('combobox', {name: 'googleSheets_3 connection'})).toHaveTextContent(
            'Ops Sheets'
        );

        await userEvent.click(within(rows[2]).getByRole('button', {name: 'Use group connection'}));

        expect(onChangesRequest).toHaveBeenCalledWith([
            {
                connectionId: 7,
                previousConnectionId: 9,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_3',
            },
        ]);
    });

    it('shows no Override badge on a tie', async () => {
        renderGroup({group: createGroup([7, 9])});

        await userEvent.click(screen.getByRole('button', {name: 'Show tasks'}));

        expect(screen.queryByText('Override')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Use group connection'})).not.toBeInTheDocument();
    });

    it('changes and clears a single task from its row', async () => {
        renderGroup({group: createGroup([7, 7])});

        await userEvent.click(screen.getByRole('button', {name: 'Show tasks'}));

        const rowSelect = screen.getByRole('combobox', {name: 'googleSheets_2 connection'});

        await pickOption(rowSelect, 'Ops Sheets');

        expect(onChangesRequest).toHaveBeenLastCalledWith([
            {
                connectionId: 9,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
        ]);

        await pickOption(screen.getByRole('combobox', {name: 'googleSheets_2 connection'}), 'Select...');

        expect(onChangesRequest).toHaveBeenLastCalledWith([
            {
                connectionId: undefined,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
        ]);
        expect(onChangesRequest).toHaveBeenCalledTimes(2);
    });

    it('shows the Choose Connection... placeholder on an empty row and offers no Select... item there', async () => {
        renderGroup({group: createGroup([7, undefined])});

        await userEvent.click(screen.getByRole('button', {name: 'Show tasks'}));

        const emptyRowSelect = screen.getByRole('combobox', {name: 'googleSheets_2 connection'});

        expect(emptyRowSelect).toHaveTextContent('Choose Connection...');

        await userEvent.click(emptyRowSelect);

        await screen.findByRole('option', {name: 'Finance Sheets'});

        expect(screen.queryByRole('option', {name: 'Select...'})).not.toBeInTheDocument();
    });

    it('creates a connection and applies it to the group', async () => {
        renderGroup({group: createGroup([7, undefined])});

        expect(hoisted.useGetComponentDefinitionQueryMock).not.toHaveBeenCalledWith(expect.anything(), true);

        await userEvent.click(screen.getByRole('button', {name: 'Create Google Sheets connection'}));

        await userEvent.click(await screen.findByRole('button', {name: 'Save connection'}));

        await waitFor(() => expect(onChangesRequest).toHaveBeenCalledTimes(1));

        expect(hoisted.useGetComponentDefinitionQueryMock).toHaveBeenCalledWith(
            {componentName: 'googleSheets', componentVersion: 1},
            true
        );
        expect(queryClient.invalidateQueries).toHaveBeenCalledWith({queryKey: ['connections']});
        expect(vi.mocked(queryClient.invalidateQueries).mock.invocationCallOrder[0]).toBeLessThan(
            onChangesRequest.mock.invocationCallOrder[0]
        );
        expect(onChangesRequest).toHaveBeenCalledWith([
            {
                connectionId: 42,
                previousConnectionId: 7,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_1',
            },
            {
                connectionId: 42,
                previousConnectionId: undefined,
                workflowConnectionKey: 'googleSheets',
                workflowNodeName: 'googleSheets_2',
            },
        ]);
    });

    it('offers Create connection when the connector has no connections yet', () => {
        hoisted.connections = [];

        renderGroup({group: createGroup([undefined, undefined])});

        expect(screen.getByRole('button', {name: 'Create connection'})).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Google Sheets connection'})).not.toBeInTheDocument();
    });

    it('hides creation when connectionDialogAllowed is false', () => {
        useWorkflowNodeDetailsPanelStore.setState({connectionDialogAllowed: false});

        const {unmount} = renderGroup({group: createGroup([7, undefined])});

        expect(getGroupSelect()).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Google Sheets connection'})).not.toBeInTheDocument();

        unmount();

        hoisted.connections = [];

        renderGroup({group: createGroup([undefined])});

        expect(screen.getByText('No connections available.')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create connection'})).not.toBeInTheDocument();
    });

    it('marks each task as needing a connection instead of offering an empty dropdown', async () => {
        hoisted.connections = [];

        renderGroup({group: createGroup([undefined, undefined])});

        await userEvent.click(screen.getByRole('button', {name: 'Show tasks'}));

        expect(screen.getAllByText('Needs a connection')).toHaveLength(2);
        expect(screen.queryByRole('combobox', {name: 'googleSheets_1 connection'})).not.toBeInTheDocument();
    });

    it('disables every control while disabled is true', async () => {
        renderGroup({disabled: true, group: createGroup([7, 7, 9, undefined])});

        expect(getGroupSelect()).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Create Google Sheets connection'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Use for all Google Sheets tasks'})).toBeDisabled();

        await userEvent.click(screen.getByRole('button', {name: 'Show tasks'}));

        for (const rowSelect of screen.getAllByRole('combobox', {name: /^googleSheets_\d connection$/})) {
            expect(rowSelect).toBeDisabled();
        }

        expect(screen.getByRole('button', {name: 'Use group connection'})).toBeDisabled();
    });

    it('marks the group busy while its batch runs', () => {
        const {rerender} = renderGroup({group: createGroup([7])});

        expect(screen.getByRole('group', {name: 'Google Sheets'})).toHaveAttribute('aria-busy', 'false');

        rerender(
            <WorkflowConnectionsGroup
                disabled
                group={createGroup([7])}
                onChangesRequest={onChangesRequest}
                pending
                title="Google Sheets"
            />
        );

        expect(screen.getByRole('group', {name: 'Google Sheets'})).toHaveAttribute('aria-busy', 'true');
    });
});
