import {AutomationWorkflowProjectConfigurationsQuery} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectConfigurationWorkflowDialog from '../AutomationWorkflowProjectConfigurationWorkflowDialog';

const mocks = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    providableValuesData: {current: undefined as unknown},
    providableValuesQuery: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAutomationWorkflowProjectConfigurationWorkflowProvidableValuesQuery: (variables: unknown) => {
        mocks.providableValuesQuery(variables);

        return {data: mocks.providableValuesData.current};
    },
    useUpdateAutomationWorkflowProjectConfigurationWorkflowMutation: () => ({
        isPending: false,
        mutate: mocks.updateMutate,
    }),
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
    enabled: true,
    environmentId: '2',
    id: 'configuration-1',
    lastExecutionDate: null,
    lastModifiedDate: null,
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
        {label: 'Slack Channel', name: 'channel', required: true, type: 'STRING'},
        {label: 'Tone', name: 'tone', required: false, type: 'STRING'},
        {label: 'Retries', name: 'retries', required: false, type: 'INTEGER'},
        {label: 'Notify', name: 'notify', required: false, type: 'BOOLEAN'},
    ],
    providedConnections: null,
    providedInputs: null,
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
    });

    it('fetches the providable values of the workflow when it opens', () => {
        renderDialog();

        expect(mocks.providableValuesQuery).toHaveBeenCalledWith({id: 'configuration-1', workflowUuid: 'wf-1'});
    });

    it('shows the title, the subtitle and the connection slots and inputs of the workflow', () => {
        renderDialog();

        expect(screen.getByText('Provided Values')).toBeInTheDocument();
        expect(
            screen.getByText(
                'Connections and inputs set here are used by every connected user and cannot be changed by them.'
            )
        ).toBeInTheDocument();

        expect(screen.getByRole('combobox', {name: 'OpenAI Connection'})).toHaveTextContent('Let each user connect');
        expect(screen.getByRole('combobox', {name: 'Slack Connection'})).toBeInTheDocument();
        expect(screen.queryByRole('combobox', {name: 'Logger Connection'})).not.toBeInTheDocument();

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

        await user.type(screen.getByRole('textbox', {name: 'Retries'}), '3');
        await user.type(screen.getByRole('textbox', {name: 'Notify'}), 'true');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(mocks.updateMutate).toHaveBeenCalledTimes(1));

        expect(mocks.updateMutate.mock.calls[0][0]).toEqual({
            id: 'configuration-1',
            providedConnections: {},
            providedInputs: {notify: true, retries: 3},
            workflowUuid: 'wf-1',
        });
    });

    it('preselects the values the workflow already provides', () => {
        renderDialog({
            provided: {providedConnections: {openai: '11'}, providedInputs: {channel: '#ops'}},
        });

        expect(screen.getByRole('combobox', {name: 'OpenAI Connection'})).toHaveTextContent('Company OpenAI');
        expect(screen.getByRole('combobox', {name: 'Slack Connection'})).toHaveTextContent('Let each user connect');
        expect(screen.getByRole('textbox', {name: 'Slack Channel'})).toHaveValue('#ops');
    });

    it('saves the chosen values, leaves out the entries each user decides, and closes', async () => {
        const user = userEvent.setup();

        const onClose = vi.fn();

        mocks.updateMutate.mockImplementation((_variables, options: {onSuccess: () => void}) => options.onSuccess());

        renderDialog({onClose});

        await user.click(screen.getByRole('combobox', {name: 'OpenAI Connection'}));
        await user.click(await screen.findByRole('option', {name: 'Company OpenAI'}));

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
});
