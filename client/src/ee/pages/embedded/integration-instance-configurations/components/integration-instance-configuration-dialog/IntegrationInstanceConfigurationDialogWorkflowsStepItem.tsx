import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections';
import {useWorkflowsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {
    REQUIRED_INTERNAL_ONLY_INPUT_HINT,
    isEmptyInternalOnlyInputValue,
} from '@/ee/pages/embedded/shared/internalOnlyInputs';
import {ComponentConnection, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {getEnabledWorkflowTasks} from '@/pages/platform/workflow-editor/utils/getEnabledWorkflowTasks';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import WorkflowConfigurationCard from '@/shared/components/workflow-configuration/WorkflowConfigurationCard';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {useEffect} from 'react';
import {Control, FieldValues, FormState, UseFormSetValue, useWatch} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

export interface IntegrationInstanceConfigurationDialogWorkflowListItemProps {
    componentName: string;
    control: Control<IntegrationInstanceConfiguration>;
    disabled?: boolean;
    disabledMessage?: string;
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
    disabled,
    disabledMessage,
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

    const componentConnections: ComponentConnection[] = getEnabledWorkflowTasks(workflow)
        .flatMap((task) => task.connections ?? [])
        .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []))
        .filter((connection) => connection.componentName !== componentName);

    const inputs = (workflow.inputs ?? [])
        .filter((input) => input.internalOnly)
        .map((input) =>
            input.componentReference?.componentName === componentName
                ? {...input, componentReference: undefined}
                : input
        );

    const watchedConnections = useWatch({
        control,
        name: `integrationInstanceConfigurationWorkflows.${workflowIndex}.connections`,
    });

    const watchedInputs = useWatch({
        control,
        name: `integrationInstanceConfigurationWorkflows.${workflowIndex}.inputs`,
    }) as Record<string, unknown> | undefined;

    const emptyRequiredInputs = inputs.filter(
        (input) => input.required && isEmptyInternalOnlyInputValue(watchedInputs?.[input.name])
    );

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
            disabled={disabled}
            disabledMessage={disabledMessage}
            enabled={workflowEnabled}
            inputCount={inputs.length}
            inputsContent={
                <div className="space-y-4">
                    {emptyRequiredInputs.length > 0 && (
                        <ul className="space-y-1">
                            {emptyRequiredInputs.map((input) => (
                                <li className="text-xs text-content-destructive-primary" key={input.name}>
                                    {`${input.label || input.name}: ${REQUIRED_INTERNAL_ONLY_INPUT_HINT}`}
                                </li>
                            ))}
                        </ul>
                    )}

                    <InputConfigurationList
                        componentConnections={componentConnections}
                        configuredConnectionIds={configuredConnectionIds}
                        control={control as unknown as Control<FieldValues>}
                        controlPath={`integrationInstanceConfigurationWorkflows.${workflowIndex}.inputs`}
                        formState={formState as unknown as FormState<FieldValues>}
                        inputs={inputs}
                        workflowId={workflow.id}
                    />
                </div>
            }
            label={label}
            onEnabledChange={handleEnabledChange}
            showWorkflowToggle={showWorkflowToggle}
            switchId={`integrationInstanceConfigurationWorkflows.${workflowIndex}`}
        />
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStepItem;
