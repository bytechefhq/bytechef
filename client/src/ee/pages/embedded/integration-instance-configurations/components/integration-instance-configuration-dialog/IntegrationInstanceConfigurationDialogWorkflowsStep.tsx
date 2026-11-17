import IntegrationInstanceConfigurationDialogWorkflowsStepItem from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItem';
import {IntegrationInstanceConfiguration, Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {Control, FormState, UseFormSetValue} from 'react-hook-form';

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
    const sortedWorkflowEntries = (workflows || [])
        .map((workflow, workflowIndex) => ({workflow, workflowIndex}))
        .sort((workflowEntry, otherWorkflowEntry) =>
            (workflowEntry.workflow.label || '').localeCompare(otherWorkflowEntry.workflow.label || '', undefined, {
                sensitivity: 'base',
            })
        );

    return (
        <div className="min-h-full space-y-5">
            {sortedWorkflowEntries.map(({workflow, workflowIndex}) => (
                <IntegrationInstanceConfigurationDialogWorkflowsStepItem
                    componentName={componentName}
                    control={control}
                    formState={formState}
                    key={workflow.id!}
                    label={workflow.label!}
                    setValue={setValue}
                    showWorkflowToggle
                    workflow={workflow}
                    workflowIndex={workflowIndex}
                />
            ))}
        </div>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStep;
