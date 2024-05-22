import IntegrationInstanceConfigurationDialogWorkflowsStepItem from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogWorkflowsStepItem';
import {IntegrationInstanceConfigurationModel, WorkflowModel} from '@/shared/middleware/embedded/configuration';
import {Control, UseFormSetValue} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';

export interface IntegrationInstanceConfigurationDialogWorkflowsStepProps {
    control: Control<IntegrationInstanceConfigurationModel>;
    formState: FormState<IntegrationInstanceConfigurationModel>;
    setValue: UseFormSetValue<IntegrationInstanceConfigurationModel>;
    workflows: WorkflowModel[];
}

const IntegrationInstanceConfigurationDialogWorkflowsStep = ({
    control,
    formState,
    setValue,
    workflows,
}: IntegrationInstanceConfigurationDialogWorkflowsStepProps) => {
    return (
        <ul className="h-full space-y-4">
            {workflows?.map((workflow, workflowIndex) => (
                <IntegrationInstanceConfigurationDialogWorkflowsStepItem
                    control={control}
                    formState={formState}
                    key={workflow.id!}
                    label={workflow.label!}
                    setValue={setValue}
                    workflow={workflow}
                    workflowIndex={workflowIndex}
                />
            ))}
        </ul>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStep;
