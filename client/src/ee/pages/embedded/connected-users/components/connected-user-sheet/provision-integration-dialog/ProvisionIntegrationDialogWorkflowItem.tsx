import McpToolBadge from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/McpToolBadge';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import WorkflowConfigurationCard from '@/shared/components/workflow-configuration/WorkflowConfigurationCard';
import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {Control, FieldValues, FormState, useWatch} from 'react-hook-form';

interface ProvisionIntegrationDialogWorkflowItemProps {
    adminConnectionCount?: number;
    control: Control<FieldValues>;
    formState: FormState<FieldValues>;
    mcpTool?: boolean;
    onEnabledChange: (enabled: boolean) => void;
    workflow: Workflow;
    workflowIndex: number;
}

const ProvisionIntegrationDialogWorkflowItem = ({
    adminConnectionCount = 0,
    control,
    formState,
    mcpTool = false,
    onEnabledChange,
    workflow,
    workflowIndex,
}: ProvisionIntegrationDialogWorkflowItemProps) => {
    const enabled = useWatch({control, name: `workflows.${workflowIndex}.enabled`});

    const inputs = (workflow.inputs ?? []).filter((input) => !input.internalOnly) as unknown as WorkflowInput[];

    const label = workflow.label || workflow.id;

    return (
        <WorkflowConfigurationCard
            badge={mcpTool && <McpToolBadge />}
            connectionCount={1 + adminConnectionCount}
            connectionsContent={
                <p className="text-sm">
                    {adminConnectionCount > 0
                        ? `Uses the connection chosen above and ${adminConnectionCount} set by an admin on the configuration.`
                        : 'Uses the connection chosen above.'}
                </p>
            }
            enabled={!!enabled}
            forceMountContent
            inputCount={inputs.length}
            inputsContent={
                <WorkflowMockProvider>
                    <InputConfigurationList
                        control={control}
                        controlPath={`workflows.${workflowIndex}.inputs`}
                        formState={formState}
                        inputs={inputs}
                        workflowId={workflow.id}
                    />
                </WorkflowMockProvider>
            }
            label={label}
            onEnabledChange={onEnabledChange}
            showWorkflowToggle
            switchId={`provision-integration-workflow-${workflowIndex}`}
        />
    );
};

export default ProvisionIntegrationDialogWorkflowItem;
