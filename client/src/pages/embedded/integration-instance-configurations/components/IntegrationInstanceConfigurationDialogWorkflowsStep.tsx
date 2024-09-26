import IntegrationInstanceConfigurationDialogWorkflowsStepItem from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogWorkflowsStepItem';
import {IntegrationInstanceConfiguration, Workflow} from '@/shared/middleware/embedded/configuration';
import {Control, UseFormSetValue} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';

export interface IntegrationInstanceConfigurationDialogWorkflowsStepProps {
    componentName: string;
    control: Control<IntegrationInstanceConfiguration>;
    formState: FormState<IntegrationInstanceConfiguration>;
    setValue: UseFormSetValue<IntegrationInstanceConfiguration>;
    workflows: Workflow[];
}

const IntegrationInstanceConfigurationDialogWorkflowsStep = ({
    componentName,
    control,
    formState,
    setValue,
    workflows,
}: IntegrationInstanceConfigurationDialogWorkflowsStepProps) => {
    return (
        <ul className="h-full space-y-4">
            {workflows?.map((workflow, workflowIndex) => (
                <IntegrationInstanceConfigurationDialogWorkflowsStepItem
                    componentName={componentName}
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
