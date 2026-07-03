import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationDialogWorkflowsStepItem from './IntegrationInstanceConfigurationDialogWorkflowsStepItem';

const googleSheetsComponentDefinition = {
    inputs: [
        {
            label: 'Sheet',
            name: 'sheetSelection',
            properties: [{controlType: 'SELECT', label: 'Spreadsheet', name: 'spreadsheetId', type: 'STRING'}],
        },
    ],
    name: 'googleSheets',
    version: 1,
};

const slackComponentDefinition = {
    inputs: [
        {
            name: 'channel',
            properties: [{controlType: 'SELECT', label: 'Channel', name: 'channelId', type: 'STRING'}],
        },
    ],
    name: 'slack',
    version: 1,
};

vi.mock('@/shared/middleware/platform/configuration', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/platform/configuration')>();

    return {
        ...actual,
        ComponentDefinitionApi: class {
            getComponentDefinition(request: {componentName: string}) {
                if (request.componentName === 'slack') {
                    return Promise.resolve(slackComponentDefinition);
                }

                if (request.componentName === 'googleSheets') {
                    return Promise.resolve(googleSheetsComponentDefinition);
                }

                return Promise.reject(new Error(`Unexpected component: ${request.componentName}`));
            }
        },
    };
});

vi.mock('@/shared/queries/platform/workflowNodeOptions.queries', () => ({
    useGetWorkflowNodeOptionsQuery: vi.fn(() => ({data: [], isLoading: false})),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn(() => 1),
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections',
    () => ({
        default: () => <div data-testid="connections" />,
    })
);

// The real InputConfigurationList renders single-property references through Properties (which surfaces the property
// label) and renders group references with their own group header. Stub Properties to render each property label as
// plain text so the test asserts on labels without pulling the full property renderer tree (and its stores) in.
vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: ({properties}: {properties: Array<{label?: string; name?: string}>}) => (
        <div data-testid="properties">
            {properties.map((property, index) => (
                <div key={index}>{property.label ?? property.name}</div>
            ))}
        </div>
    ),
}));

vi.mock('react-inlinesvg', () => ({
    default: () => <svg data-testid="inline-svg" />,
}));

vi.mock('@/assets/subflow.svg', () => ({default: 'subflow.svg'}));

const workflow = {
    id: 'wf1',
    inputs: [
        {
            componentReference: {componentName: 'slack', componentVersion: 1, groupName: 'channel'},
            internalOnly: true,
            label: 'Channel',
            name: 'channel',
        },
        {
            componentReference: {componentName: 'googleSheets', componentVersion: 1, groupName: 'sheetSelection'},
            internalOnly: true,
            name: 'sheet',
        },
    ],
    tasks: [],
    triggers: [],
} as unknown as Workflow;

const QueryWrapper = ({children}: {children: ReactNode}) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
};

const TestForm = ({componentName = 'someApp'}: {componentName?: string}) => {
    const {control, formState, setValue} = useForm<IntegrationInstanceConfiguration>({
        defaultValues: {
            integrationInstanceConfigurationWorkflows: [
                {connections: [], inputs: {}},
            ] as IntegrationInstanceConfiguration['integrationInstanceConfigurationWorkflows'],
        },
    });

    return (
        <IntegrationInstanceConfigurationDialogWorkflowsStepItem
            componentName={componentName}
            control={control}
            formState={formState}
            label="Workflow"
            setValue={setValue}
            showWorkflowToggle={false}
            workflow={workflow}
            workflowIndex={0}
        />
    );
};

const workflowWithInternalOnlyInputs = {
    id: 'wf2',
    inputs: [
        {internalOnly: true, label: 'API Key', name: 'apiKey', type: 'string'},
        {internalOnly: false, label: 'Channel', name: 'channel', type: 'string'},
        {label: 'Legacy', name: 'legacy', type: 'string'},
    ],
    tasks: [],
    triggers: [],
} as unknown as Workflow;

const TestFormWithInternalOnlyInputs = () => {
    const {control, formState, setValue} = useForm<IntegrationInstanceConfiguration>({
        defaultValues: {
            integrationInstanceConfigurationWorkflows: [
                {connections: [], inputs: {}},
            ] as IntegrationInstanceConfiguration['integrationInstanceConfigurationWorkflows'],
        },
    });

    return (
        <IntegrationInstanceConfigurationDialogWorkflowsStepItem
            componentName="someApp"
            control={control}
            formState={formState}
            label="Workflow"
            setValue={setValue}
            showWorkflowToggle={false}
            workflow={workflowWithInternalOnlyInputs}
            workflowIndex={0}
        />
    );
};

describe('IntegrationInstanceConfigurationDialogWorkflowsStepItem', () => {
    it('renders component-defined inputs through the shared InputConfigurationList', async () => {
        render(
            <QueryWrapper>
                <TestForm />
            </QueryWrapper>
        );

        await waitFor(() => expect(screen.getByText('Channel')).toBeInTheDocument());
        await waitFor(() => expect(screen.getByText('Spreadsheet')).toBeInTheDocument());
    });

    it('excludes inputs that reference the integration component', async () => {
        render(
            <QueryWrapper>
                <TestForm componentName="slack" />
            </QueryWrapper>
        );

        await waitFor(() => expect(screen.getByText('Spreadsheet')).toBeInTheDocument());

        expect(screen.queryByText('Channel')).not.toBeInTheDocument();
    });

    it('passes only internalOnly inputs to the inputs list', async () => {
        render(
            <QueryWrapper>
                <TestFormWithInternalOnlyInputs />
            </QueryWrapper>
        );

        await waitFor(() => expect(screen.getByText('API Key')).toBeInTheDocument());

        expect(screen.queryByText('Channel')).not.toBeInTheDocument();
        expect(screen.queryByText('Legacy')).not.toBeInTheDocument();
    });
});
