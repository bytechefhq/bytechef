import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections';
import {useWorkflowsEnabledStore} from '@/ee/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {ComponentConnection, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {useEffect} from 'react';
import {Control, FormState, UseFormSetValue} from 'react-hook-form';
import {useShallow} from 'zustand/react/shallow';

export interface IntegrationInstanceConfigurationDialogWorkflowListItemProps {
    componentName: string;
    control: Control<IntegrationInstanceConfiguration>;
    formState: FormState<IntegrationInstanceConfiguration>;
    label: string;
    setValue: UseFormSetValue<IntegrationInstanceConfiguration>;
    switchHidden?: boolean;
    workflow: Workflow;
    workflowIndex: number;
}

const IntegrationInstanceConfigurationDialogWorkflowsStepItem = ({
    componentName,
    control,
    /* eslint-disable @typescript-eslint/no-unused-vars */
    formState,
    label,
    setValue,
    switchHidden = false,
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

    useEffect(() => {
        setValue(`integrationInstanceConfigurationWorkflows.${workflowIndex!}.workflowId`, workflow.id!);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <div>
            {!switchHidden && (
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

            {(workflowEnabledMap.get(workflow.id!) || switchHidden) && (
                <div className="mt-2 space-y-6">
                    {/*<div className="flex flex-col gap-3">*/}

                    {/*    <Label className="font-semibold">Inputs</Label>*/}

                    {/*    <IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs*/}

                    {/*        control={control}*/}

                    {/*        formState={formState}*/}

                    {/*        workflow={workflow}*/}

                    {/*        workflowIndex={workflowIndex}*/}

                    {/*    />*/}

                    {/*</div>*/}

                    <div className="flex flex-col gap-3">
                        <Label className="font-semibold">Connections</Label>

                        <IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections
                            componentConnections={componentConnections}
                            control={control}
                            workflowIndex={workflowIndex}
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStepItem;
