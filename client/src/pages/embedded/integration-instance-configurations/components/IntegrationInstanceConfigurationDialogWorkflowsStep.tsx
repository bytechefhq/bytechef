import {IntegrationInstanceConfigurationModel} from '@/middleware/embedded/configuration';
import IntegrationInstanceConfigurationDialogWorkflowsStepItem from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogWorkflowsStepItem';
import {useGetIntegrationWorkflowsQuery} from '@/queries/embedded/integrationWorkflows.queries';
import {Control, UseFormGetValues, UseFormRegister, UseFormSetValue} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';

export interface IntegrationInstanceConfigurationDialogWorkflowsStepProps {
    control: Control<IntegrationInstanceConfigurationModel>;
    formState: FormState<IntegrationInstanceConfigurationModel>;
    getValues: UseFormGetValues<IntegrationInstanceConfigurationModel>;
    register: UseFormRegister<IntegrationInstanceConfigurationModel>;
    setValue: UseFormSetValue<IntegrationInstanceConfigurationModel>;
}

const IntegrationInstanceConfigurationDialogWorkflowsStep = ({
    control,
    formState,
    getValues,
    register,
    setValue,
}: IntegrationInstanceConfigurationDialogWorkflowsStepProps) => {
    const {data: workflows} = useGetIntegrationWorkflowsQuery(getValues().integrationId!);

    return (
        <ul className="h-full space-y-4">
            {workflows?.map((workflow, workflowIndex) => (
                <IntegrationInstanceConfigurationDialogWorkflowsStepItem
                    control={control}
                    formState={formState}
                    key={workflow.id!}
                    label={workflow.label!}
                    register={register}
                    setValue={setValue}
                    workflowId={workflow.id!}
                    workflowIndex={workflowIndex}
                />
            ))}
        </ul>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStep;
