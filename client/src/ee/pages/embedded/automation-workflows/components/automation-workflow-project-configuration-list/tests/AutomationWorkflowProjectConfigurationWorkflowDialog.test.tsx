import {AutomationWorkflowProjectConfigurationsQuery} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationWorkflowDialog from '../AutomationWorkflowProjectConfigurationWorkflowDialog';

const mocks = vi.hoisted(() => ({
    componentDefinitionData: {current: undefined as unknown},
    inputOptionsData: {current: undefined as unknown},
    inputOptionsQuery: vi.fn(),
    inputOptionsState: {current: {isError: false, isLoading: false}},
    invalidateQueries: vi.fn(),
    providableValuesData: {current: undefined as unknown},
    providableValuesQuery: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationWorkflowInputOptionsQuery: (
        variables: unknown,
        options: {enabled?: boolean}
    ) => {
        mocks.inputOptionsQuery(variables, options);

        return {
            data: options.enabled ? mocks.inputOptionsData.current : undefined,
            ...mocks.inputOptionsState.current,
        };
    },
    useAutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery: (variables: unknown) => {
        mocks.providableValuesQuery(variables);

        return {data: mocks.providableValuesData.current};
    },
    useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation: () => ({
        isPending: false,
        mutate: mocks.updateMutate,
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: mocks.componentDefinitionData.current, isLoading: false}),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: mocks.invalidateQueries}),
}));

type ConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type WorkflowType = ConfigurationType['workflows'][number];

const configuration: ConfigurationType = {
    automationHubVisible: true,
    connectedUserCount: 0,
    enabled: true,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
    name: 'STAGING',
    permissionExpression: null,
    projectId: 'project-1',
    projectVersion: 2,
    workflows: [],
};

const workflow: WorkflowType = {
    enabled: true,
    label: 'Summarize Email',
    lastExecutionDate: null,
    providedConnections: null,
    providedInputs: null,
    workflowId: 'pinned-wf-1',
    workflowUuid: 'wf-1',
};

const providableValues = {
    connectionSlots: [
        {
            componentName: 'openai',
            providableConnections: [
                {id: '11', name: 'Company OpenAI'},
                {id: '12', name: 'Backup OpenAI'},
            ],
            required: true,
            title: 'OpenAI',
        },
        {
            componentName: 'slack',
            providableConnections: [{id: '21', name: 'Company Slack'}],
            required: false,
            title: 'Slack',
        },
    ],
    inputs: [
        {label: 'Slack Channel', name: 'channel', optionConnections: [], required: true, type: 'STRING'},
        {label: 'Tone', name: 'tone', optionConnections: [], required: false, type: 'STRING'},
        {label: 'Retries', name: 'retries', optionConnections: [], required: false, type: 'INTEGER'},
        {label: 'Notify', name: 'notify', optionConnections: [], required: false, type: 'BOOLEAN'},
    ],
    providedConnections: null,
    providedInputs: null,
};

const slackChannelReferenceInput = {
    componentName: 'slack',
    componentVersion: 1,
    groupName: 'channel',
    label: 'Slack Channel',
    name: 'channel',
    optionConnections: [
        {id: '31', name: 'Company Slack'},
        {id: '32', name: 'Support Slack'},
    ],
    required: true,
    type: 'STRING',
};

const slackComponentDefinition = (property: Record<string, unknown>) => ({
    inputs: [{label: 'Channel', name: 'channel', properties: [{label: 'Channel', name: 'channel', ...property}]}],
});

const slackMessageReferenceInput = {
    ...slackChannelReferenceInput,
    groupName: 'message',
    label: 'Slack Message',
    name: 'message',
};

const slackMessageComponentDefinition = {
    inputs: [
        {
            label: 'Message',
            name: 'message',
            properties: [
                {label: 'Text', name: 'text', type: 'STRING'},
                {label: 'Urgent', name: 'urgent', type: 'BOOLEAN'},
                {
                    label: 'Thread Channel',
                    name: 'threadChannel',
                    optionsDataSource: {optionsLookupDependsOn: ['message.text']},
                    type: 'STRING',
                },
            ],
        },
    ],
};

const openInputsTab = async (user: ReturnType<typeof userEvent.setup>) => {
    await user.click(screen.getByRole('tab', {name: /Inputs/}));
};

const renderDialog = (overrides: {onClose?: () => void; provided?: Record<string, unknown>} = {}) => {
    mocks.providableValuesData.current = {
        automationWorkflowProjectConfigurationWorkflowProvidableValues: {...providableValues, ...overrides.provided},
    };

    return render(
        <AutomationWorkflowProjectConfigurationWorkflowDialog
            configuration={configuration}
            onClose={overrides.onClose ?? vi.fn()}
            workflow={workflow}
        />
    );
};

describe('AutomationWorkflowProjectConfigurationWorkflowDialog', () => {
    beforeEach(() => {
        windowResizeObserver();

        Element.prototype.hasPointerCapture = vi.fn();
        Element.prototype.releasePointerCapture = vi.fn();
        Element.prototype.scrollIntoView = vi.fn();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();

        mocks.componentDefinitionData.current = undefined;
        mocks.inputOptionsData.current = undefined;
        mocks.inputOptionsState.current = {isError: false, isLoading: false};
    });

    it('fetches the providable values of the workflow when it opens', () => {
        renderDialog();

        expect(mocks.providableValuesQuery).toHaveBeenCalledWith({id: 'configuration-1', workflowUuid: 'wf-1'});
    });

    it('shows the title, the subtitle and the connection slots and inputs of the workflow on their tabs', async () => {
        const user = userEvent.setup();

        renderDialog();

        expect(screen.getByText('Provided Values')).toBeInTheDocument();
        expect(
            screen.getByText(
                'Connections and inputs set here are used by every connected user and cannot be changed by them.'
            )
        ).toBeInTheDocument();

        expect(screen.getByRole('tab', {name: /Connections\s*\(2\)/})).toHaveAttribute('data-state', 'active');
        expect(screen.getByRole('tab', {name: /Inputs\s*\(4\)/})).toBeInTheDocument();

        expect(screen.getByRole('combobox', {name: 'OpenAI Connection'})).toHaveTextContent('Let each user connect');
        expect(screen.getByRole('combobox', {name: 'Slack Connection'})).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Logger Connection'})).not.toBeInTheDocument();
        expect(screen.queryByRole('textbox', {name: 'Slack Channel'})).not.toBeInTheDocument();

        await openInputsTab(user);

        expect(screen.getByRole('textbox', {name: 'Slack Channel'})).toHaveValue('');
        expect(screen.getByRole('textbox', {name: 'Tone'})).toHaveValue('');
    });

    it('lists only the connections the workflow slot offers', async () => {
        const user = userEvent.setup();

        renderDialog();

        await user.click(screen.getByRole('combobox', {name: 'OpenAI Connection'}));

        expect(await screen.findByRole('option', {name: 'Let each user connect'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Company OpenAI'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Backup OpenAI'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Company Slack'})).not.toBeInTheDocument();
    });

    it('labels a preselected connection that is no longer providable', () => {
        renderDialog({provided: {providedConnections: {openai: '99'}}});

        expect(screen.getByRole('combobox', {name: 'OpenAI Connection'})).toHaveTextContent(
            'Connection 99 (unavailable)'
        );
    });

    it('sends numeric and boolean inputs with their types', async () => {
        const user = userEvent.setup();

        renderDialog();

        await openInputsTab(user);

        await user.type(screen.getByRole('textbox', {name: 'Retries'}), '3');

        await user.click(screen.getByRole('combobox', {name: 'Notify'}));
        await user.click(await screen.findByRole('option', {name: 'True'}));

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0]).toEqual({
            id: 'configuration-1',
            providedConnections: {},
            providedInputs: {notify: true, retries: 3},
            workflowUuid: 'wf-1',
        });
    });

    it('preselects the values the workflow already provides', async () => {
        const user = userEvent.setup();

        renderDialog({
            provided: {providedConnections: {openai: '11'}, providedInputs: {channel: '#ops'}},
        });

        expect(screen.getByRole('combobox', {name: 'OpenAI Connection'})).toHaveTextContent('Company OpenAI');
        expect(screen.getByRole('combobox', {name: 'Slack Connection'})).toHaveTextContent('Let each user connect');

        await openInputsTab(user);

        expect(screen.getByRole('textbox', {name: 'Slack Channel'})).toHaveValue('#ops');
    });

    it('saves the chosen values, leaves out the entries each user decides, and closes', async () => {
        const user = userEvent.setup();

        const onClose = vi.fn();

        mocks.updateMutate.mockImplementation((_variables, options: {onSuccess: () => void}) => options.onSuccess());

        renderDialog({onClose});

        await user.click(screen.getByRole('combobox', {name: 'OpenAI Connection'}));
        await user.click(await screen.findByRole('option', {name: 'Company OpenAI'}));

        await openInputsTab(user);

        await user.type(screen.getByRole('textbox', {name: 'Slack Channel'}), '#ops');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0]).toEqual({
            id: 'configuration-1',
            providedConnections: {openai: '11'},
            providedInputs: {channel: '#ops'},
            workflowUuid: 'wf-1',
        });
        expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationWorkflowProjectConfigurations']});
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('clears a provided connection when each user connects again', async () => {
        const user = userEvent.setup();

        renderDialog({provided: {providedConnections: {openai: '11'}}});

        await user.click(screen.getByRole('combobox', {name: 'OpenAI Connection'}));
        await user.click(await screen.findByRole('option', {name: 'Let each user connect'}));

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0]).toEqual({
            id: 'configuration-1',
            providedConnections: {},
            providedInputs: {},
            workflowUuid: 'wf-1',
        });
    });

    it('renders the static options of a component input as a select and saves the chosen one', async () => {
        const user = userEvent.setup();

        mocks.componentDefinitionData.current = slackComponentDefinition({
            options: [
                {label: 'General', value: 'C1'},
                {label: 'Random', value: 'C2'},
            ],
            type: 'STRING',
        });

        renderDialog({provided: {inputs: [slackChannelReferenceInput]}});

        await openInputsTab(user);

        await user.click(screen.getByRole('combobox', {name: 'Slack Channel'}));

        expect(await screen.findByRole('option', {name: 'Let each user fill it'})).toBeInTheDocument();

        await user.click(screen.getByRole('option', {name: 'Random'}));
        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0].providedInputs).toEqual({channel: 'C2'});
        expect(mocks.inputOptionsQuery).not.toHaveBeenCalled();
    });

    it('loads dynamic options with the connection provided for the component', async () => {
        const user = userEvent.setup();

        mocks.componentDefinitionData.current = slackComponentDefinition({optionsDataSource: {}, type: 'STRING'});
        mocks.inputOptionsData.current = {
            automationWorkflowProjectConfigurationWorkflowInputOptions: [
                {description: null, label: 'ops', value: 'C9'},
            ],
        };

        renderDialog({provided: {inputs: [slackChannelReferenceInput], providedConnections: {slack: '21'}}});

        await openInputsTab(user);

        expect(screen.queryByRole('combobox', {name: 'Load Slack Channel options with'})).not.toBeInTheDocument();
        expect(mocks.inputOptionsQuery).toHaveBeenLastCalledWith(
            {
                connectionId: '21',
                id: 'configuration-1',
                inputName: 'channel',
                lookupDependsOnValues: {},
                propertyName: 'channel',
                workflowUuid: 'wf-1',
            },
            {enabled: true}
        );

        await user.click(screen.getByRole('combobox', {name: 'Slack Channel'}));
        await user.click(await screen.findByRole('option', {name: 'ops'}));
        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0]).toEqual({
            id: 'configuration-1',
            providedConnections: {slack: '21'},
            providedInputs: {channel: 'C9'},
            workflowUuid: 'wf-1',
        });
    });

    it('loads dynamic options with a picked connection when none is provided, without saving it', async () => {
        const user = userEvent.setup();

        mocks.componentDefinitionData.current = slackComponentDefinition({optionsDataSource: {}, type: 'STRING'});
        mocks.inputOptionsData.current = {
            automationWorkflowProjectConfigurationWorkflowInputOptions: [
                {description: null, label: 'support', value: 'C7'},
            ],
        };

        renderDialog({provided: {connectionSlots: [], inputs: [slackChannelReferenceInput]}});

        await openInputsTab(user);

        const optionsConnectionSelect = screen.getByRole('combobox', {name: 'Load Slack Channel options with'});

        expect(optionsConnectionSelect).toHaveTextContent('Company Slack');

        await user.click(optionsConnectionSelect);
        await user.click(await screen.findByRole('option', {name: 'Support Slack'}));

        expect(mocks.inputOptionsQuery).toHaveBeenLastCalledWith(
            expect.objectContaining({connectionId: '32', inputName: 'channel'}),
            {enabled: true}
        );

        await user.click(screen.getByRole('combobox', {name: 'Slack Channel'}));
        await user.click(await screen.findByRole('option', {name: 'support'}));
        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0]).toEqual({
            id: 'configuration-1',
            providedConnections: {},
            providedInputs: {channel: 'C7'},
            workflowUuid: 'wf-1',
        });
    });

    it('keeps a provided value that is not among the loaded options selectable', async () => {
        const user = userEvent.setup();

        mocks.componentDefinitionData.current = slackComponentDefinition({optionsDataSource: {}, type: 'STRING'});
        mocks.inputOptionsData.current = {automationWorkflowProjectConfigurationWorkflowInputOptions: []};

        renderDialog({
            provided: {connectionSlots: [], inputs: [slackChannelReferenceInput], providedInputs: {channel: 'C404'}},
        });

        await openInputsTab(user);

        expect(screen.getByRole('combobox', {name: 'Slack Channel'})).toHaveTextContent('C404');
    });

    it('edits every member of a multi-member component input group and saves them as one map', async () => {
        const user = userEvent.setup();

        mocks.componentDefinitionData.current = slackMessageComponentDefinition;
        mocks.inputOptionsData.current = {
            automationWorkflowProjectConfigurationWorkflowInputOptions: [
                {description: null, label: 'ops thread', value: 'T1'},
            ],
        };

        renderDialog({
            provided: {
                connectionSlots: [],
                inputs: [slackMessageReferenceInput],
                providedInputs: {message: {text: 'Hi'}},
            },
        });

        await openInputsTab(user);

        expect(screen.getByRole('group', {name: 'Slack Message'})).toBeInTheDocument();
        expect(screen.getByRole('textbox', {name: 'Text'})).toHaveValue('Hi');
        expect(mocks.inputOptionsQuery).toHaveBeenLastCalledWith(
            expect.objectContaining({
                inputName: 'message',
                lookupDependsOnValues: {text: 'Hi'},
                propertyName: 'threadChannel',
            }),
            {enabled: true}
        );

        await user.click(screen.getByRole('combobox', {name: 'Urgent'}));
        await user.click(await screen.findByRole('option', {name: 'True'}));

        await user.click(screen.getByRole('combobox', {name: 'Thread Channel'}));
        await user.click(await screen.findByRole('option', {name: 'ops thread'}));

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0].providedInputs).toEqual({
            message: {text: 'Hi', threadChannel: 'T1', urgent: 'true'},
        });
    });

    it('leaves out a multi-member component input group whose members are all empty', async () => {
        const user = userEvent.setup();

        mocks.componentDefinitionData.current = slackMessageComponentDefinition;

        renderDialog({
            provided: {
                connectionSlots: [],
                inputs: [slackMessageReferenceInput],
                providedInputs: {message: {text: 'Hi'}},
            },
        });

        await openInputsTab(user);

        await user.clear(screen.getByRole('textbox', {name: 'Text'}));
        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0].providedInputs).toEqual({});
    });

    it('falls back to a text box when the component definition has no matching input group', async () => {
        const user = userEvent.setup();

        mocks.componentDefinitionData.current = {inputs: []};

        renderDialog({provided: {connectionSlots: [], inputs: [slackChannelReferenceInput]}});

        await openInputsTab(user);

        expect(screen.getByRole('textbox', {name: 'Slack Channel'})).toHaveValue('');
    });

    it('explains why dynamic options are not shown', async () => {
        const user = userEvent.setup();

        mocks.componentDefinitionData.current = slackComponentDefinition({optionsDataSource: {}, type: 'STRING'});

        const {unmount} = renderDialog({
            provided: {connectionSlots: [], inputs: [{...slackChannelReferenceInput, optionConnections: []}]},
        });

        await openInputsTab(user);

        expect(screen.getByRole('combobox', {name: 'Slack Channel'})).toHaveTextContent(
            'Let each user fill it (no connection to load options)'
        );
        expect(screen.queryByRole('combobox', {name: 'Load Slack Channel options with'})).not.toBeInTheDocument();

        unmount();

        mocks.inputOptionsState.current = {isError: false, isLoading: true};

        const {unmount: unmountLoading} = renderDialog({
            provided: {connectionSlots: [], inputs: [slackChannelReferenceInput]},
        });

        await openInputsTab(user);

        expect(screen.getByRole('combobox', {name: 'Slack Channel'})).toHaveTextContent('Loading options...');

        unmountLoading();

        mocks.inputOptionsState.current = {isError: true, isLoading: false};

        renderDialog({provided: {connectionSlots: [], inputs: [slackChannelReferenceInput]}});

        await openInputsTab(user);

        expect(screen.getByRole('combobox', {name: 'Slack Channel'})).toHaveTextContent(
            'Let each user fill it (options could not be loaded)'
        );
    });

    it('says when the workflow has no connections or no inputs to provide', async () => {
        const user = userEvent.setup();

        renderDialog({provided: {connectionSlots: [], inputs: []}});

        expect(screen.getByText('This workflow has no connections to provide.')).toBeInTheDocument();

        await openInputsTab(user);

        expect(screen.getByText('This workflow has no inputs to provide.')).toBeInTheDocument();
    });

    it('closes without saving when cancelled', async () => {
        const user = userEvent.setup();

        const onClose = vi.fn();

        renderDialog({onClose});

        await user.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(onClose).toHaveBeenCalledTimes(1);
        expect(mocks.updateMutate).not.toHaveBeenCalled();
    });

    it('shows a loading state until the providable values arrive', () => {
        mocks.providableValuesData.current = undefined;

        render(
            <AutomationWorkflowProjectConfigurationWorkflowDialog
                configuration={configuration}
                onClose={vi.fn()}
                workflow={workflow}
            />
        );

        expect(screen.getByText('Loading...')).toBeInTheDocument();
    });
});
