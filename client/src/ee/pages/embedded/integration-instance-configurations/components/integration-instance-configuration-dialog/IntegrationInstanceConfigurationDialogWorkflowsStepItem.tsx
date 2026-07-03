import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections';
import {useWorkflowsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {ComponentConnection, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {useEffect} from 'react';
import {Control, FieldValues, FormState, UseFormSetValue, useWatch} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

export interface IntegrationInstanceConfigurationDialogWorkflowListItemProps {
    componentName: string;
    control: Control<IntegrationInstanceConfiguration>;
    formState: FormState<IntegrationInstanceConfiguration>;
    label: string;
    setValue: UseFormSetValue<IntegrationInstanceConfiguration>;
    showWorkflowToggle?: boolean;
    workflow: Workflow;
    workflowIndex: number;
}

const IntegrationInstanceConfigurationDialogWorkflowsStepItem = ({
    componentName,
    control,
    formState,
    label,
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

    // Inputs referencing the integration's own component are configured through the connect flow.
    // Only internalOnly inputs are configured here (the admin dialog); end-user inputs render in the ConnectDialog.
    // The REST inputs[] already carry componentReference/internalOnly (populated server-side by the workflow mappers).
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

    useEffect(() => {
        setValue(`integrationInstanceConfigurationWorkflows.${workflowIndex!}.workflowId`, workflow.id!);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <div>
            {showWorkflowToggle && (
                <div className="flex cursor-pointer justify-between py-2">
                    <span className="font-semibold">{label}</span>

                    <Switch
                        checked={workflowEnabledMap.get(workflow.id!)}
                        onCheckedChange={(value) => {
                            setValue(`integrationInstanceConfigurationWorkflows.${workflowIndex!}.enabled`, value);
                            setWorkflowEnabled(workflow.id!, value);
                        }}
                    />
                </div>
            )}

            {(workflowEnabledMap.get(workflow.id!) || !showWorkflowToggle) && (
                <ul className="mt-2 space-y-6">
                    <li className="flex flex-col gap-3">
                        <Label className="text-base font-semibold">Inputs</Label>

                        <InputConfigurationList
                            componentConnections={componentConnections}
                            configuredConnectionIds={configuredConnectionIds}
                            control={control as unknown as Control<FieldValues>}
                            controlPath={`integrationInstanceConfigurationWorkflows.${workflowIndex}.inputs`}
                            formState={formState as unknown as FormState<FieldValues>}
                            inputs={inputs}
                            workflowId={workflow.id}
                        />
                    </li>

                    <li className="flex flex-col gap-3">
                        <Label className="text-base font-semibold">Connections</Label>

                        <IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections
                            componentConnections={componentConnections}
                            control={control}
                            workflowIndex={workflowIndex}
                        />
                    </li>
                </ul>
            )}
        </div>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStepItem;
