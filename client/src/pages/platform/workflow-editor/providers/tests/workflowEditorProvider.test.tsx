import {
    ConnectionI,
    WorkflowEditorProvider,
    WorkflowEditorStateI,
    useWorkflowEditor,
} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {ComponentDefinitionBasic, Tag} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {render, screen} from '@/shared/util/test-utils';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {describe, expect, test} from 'vitest';

const buildProviderValue = (overrides: Partial<WorkflowEditorStateI> = {}): WorkflowEditorStateI => ({
    ConnectionKeys: {
        connection: () => [],
        connections: [],
        filteredConnections: () => [],
    },
    cancelWorkflowQueries: () => {},
    connectionTagsQueryKey: [],
    deleteClusterElementParameterMutation:
        {} as unknown as WorkflowEditorStateI['deleteClusterElementParameterMutation'],
    deleteWorkflowNodeParameterMutation: {} as unknown as WorkflowEditorStateI['deleteWorkflowNodeParameterMutation'],
    invalidateWorkflowQueries: () => {},
    updateClusterElementParameterMutation:
        {} as unknown as WorkflowEditorStateI['updateClusterElementParameterMutation'],
    updateWorkflowMutation: {} as unknown as UpdateWorkflowMutationType,
    updateWorkflowNodeParameterMutation: {} as unknown as WorkflowEditorStateI['updateWorkflowNodeParameterMutation'],
    useCreateConnectionMutation: () => ({}) as unknown as UseMutationResult<number, Error, ConnectionI, unknown>,
    useGetComponentDefinitionsQuery: () => ({}) as UseQueryResult<Array<ComponentDefinitionBasic>, Error>,
    useGetConnectionTagsQuery: () => ({}) as unknown as UseQueryResult<Tag[], Error>,
    useGetConnectionsQuery: () => ({}) as unknown as UseQueryResult<ConnectionI[], Error>,
    webhookTriggerTestApi: {
        startWebhookTriggerTest: async () => ({}),
        stopWebhookTriggerTest: async () => {},
    },
    ...overrides,
});

const CodeWorkflowFlagConsumer = () => {
    const {codeWorkflow, codeWorkflowLanguage} = useWorkflowEditor();

    return (
        <div>
            <span data-testid="code-workflow">{String(codeWorkflow)}</span>

            <span data-testid="code-workflow-language">{String(codeWorkflowLanguage)}</span>
        </div>
    );
};

describe('WorkflowEditorProvider', () => {
    test('exposes codeWorkflow and codeWorkflowLanguage set on the provider value', () => {
        render(
            <WorkflowEditorProvider
                value={buildProviderValue({
                    codeWorkflow: true,
                    codeWorkflowLanguage: 'PYTHON',
                })}
            >
                <CodeWorkflowFlagConsumer />
            </WorkflowEditorProvider>
        );

        expect(screen.getByTestId('code-workflow')).toHaveTextContent('true');
        expect(screen.getByTestId('code-workflow-language')).toHaveTextContent('PYTHON');
    });

    test('leaves codeWorkflow and codeWorkflowLanguage undefined when not populated by the provider value', () => {
        render(
            <WorkflowEditorProvider value={buildProviderValue()}>
                <CodeWorkflowFlagConsumer />
            </WorkflowEditorProvider>
        );

        expect(screen.getByTestId('code-workflow')).toHaveTextContent('undefined');
        expect(screen.getByTestId('code-workflow-language')).toHaveTextContent('undefined');
    });
});
