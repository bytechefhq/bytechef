import Switch from '@/components/Switch/Switch';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {Control, FieldValues, FormState, useWatch} from 'react-hook-form';

interface ProvisionIntegrationDialogWorkflowItemProps {
    control: Control<FieldValues>;
    formState: FormState<FieldValues>;
    onEnabledChange: (enabled: boolean) => void;
    workflow: Workflow;
    workflowIndex: number;
}

const ProvisionIntegrationDialogWorkflowItem = ({
    control,
    formState,
    onEnabledChange,
    workflow,
    workflowIndex,
}: ProvisionIntegrationDialogWorkflowItemProps) => {
    const enabled = useWatch({control, name: `workflows.${workflowIndex}.enabled`});

    const inputs = (workflow.inputs ?? []).filter((input) => !input.internalOnly) as unknown as WorkflowInput[];

    const label = workflow.label || workflow.id;

    return (
        <div className="space-y-3 rounded border border-border/50 px-3 py-2">
            <div className="flex items-center justify-between">
                <span className="text-sm font-medium">{label}</span>

                <Switch aria-label={label} checked={!!enabled} onCheckedChange={onEnabledChange} />
            </div>

            {enabled && inputs.length > 0 && (
                <WorkflowMockProvider>
                    <InputConfigurationList
                        control={control}
                        controlPath={`workflows.${workflowIndex}.inputs`}
                        formState={formState}
                        inputs={inputs}
                        workflowId={workflow.id}
                    />
                </WorkflowMockProvider>
            )}
        </div>
    );
};

export default ProvisionIntegrationDialogWorkflowItem;
