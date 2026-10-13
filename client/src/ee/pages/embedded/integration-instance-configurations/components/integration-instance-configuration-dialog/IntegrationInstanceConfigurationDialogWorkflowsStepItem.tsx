import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections';
import {useWorkflowsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {ComponentConnection, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import WorkflowConfigurationCard from '@/shared/components/workflow-configuration/WorkflowConfigurationCard';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {useEffect} from 'react';
import {Control, FieldValues, FormState, UseFormSetValue, useWatch} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

export interface IntegrationInstanceConfigurationDialogWorkflowListItemProps {
    componentName: string;
    control: Control<IntegrationInstanceConfiguration>;
    enabled?: boolean;
    formState: FormState<IntegrationInstanceConfiguration>;
    label: string;
    onEnabledChange?: (enabled: boolean) => void;
    setValue: UseFormSetValue<IntegrationInstanceConfiguration>;
    showWorkflowToggle?: boolean;
    workflow: Workflow;
    workflowIndex: number;
}

const IntegrationInstanceConfigurationDialogWorkflowsStepItem = ({
    componentName,
    control,
    enabled,
    formState,
    label,
    onEnabledChange,
    setValue,
    showWorkflowToggle = false,
    workflow,
    workflowIndex,
}: IntegrationInstanceConfigurationDialogWorkflowListItemProps) => {
    const [setWorkflowEnabled, workflowEnabledMap] = useWorkflowsEnabledStore(
        useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [setWorkflowEnabled, workflowEnabledMap])
    );

    const componentConnections: ComponentConnection[] = (workflow?.tasks ?? [])
        .flatMap((task) => task.connections ?? [])
        .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []))
        .filter((connection) => connection.componentName !== componentName);

    const inputs = (workflow.inputs ?? []).filter(
        (input) => input.componentReference?.componentName !== componentName && input.internalOnly
    );

    const watchedConnections = useWatch({
        control,
        name: `integrationInstanceConfigurationWorkflows.${workflowIndex}.connections`,
    });

    const configuredConnectionIds = componentConnections.map(
        (componentConnection, connectionIndex) => watchedConnections?.[connectionIndex]?.connectionId
    );

    const workflowEnabled = enabled ?? workflowEnabledMap.get(workflow.id!) ?? false;

    const handleEnabledChange = (value: boolean) => {
        if (onEnabledChange) {
            onEnabledChange(value);

            return;
        }

        setValue(`integrationInstanceConfigurationWorkflows.${workflowIndex!}.enabled`, value);

        setWorkflowEnabled(workflow.id!, value);
    };

    useEffect(() => {
        setValue(`integrationInstanceConfigurationWorkflows.${workflowIndex!}.workflowId`, workflow.id!);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <WorkflowConfigurationCard
            connectionCount={componentConnections.length}
            connectionsContent={
                <IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections
                    componentConnections={componentConnections}
                    control={control}
                    workflowIndex={workflowIndex}
                />
            }
            enabled={workflowEnabled}
            inputCount={inputs.length}
            inputsContent={
                <InputConfigurationList
                    componentConnections={componentConnections}
                    configuredConnectionIds={configuredConnectionIds}
                    control={control as unknown as Control<FieldValues>}
                    controlPath={`integrationInstanceConfigurationWorkflows.${workflowIndex}.inputs`}
                    formState={formState as unknown as FormState<FieldValues>}
                    inputs={inputs}
                    workflowId={workflow.id}
                />
            }
            label={label}
            onEnabledChange={handleEnabledChange}
            showWorkflowToggle={showWorkflowToggle}
            switchId={`integrationInstanceConfigurationWorkflows.${workflowIndex}`}
        />
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStepItem;
