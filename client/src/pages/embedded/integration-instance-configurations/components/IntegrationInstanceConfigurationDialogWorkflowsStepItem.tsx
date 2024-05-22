import {Label} from '@/components/ui/label';
import {Switch} from '@/components/ui/switch';
import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections';
import IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs';
import {useWorkflowsEnabledStore} from '@/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {WorkflowModel} from '@/shared/middleware/automation/configuration';
import {
    IntegrationInstanceConfigurationModel,
    WorkflowConnectionModel,
} from '@/shared/middleware/embedded/configuration';
import {useEffect} from 'react';
import {Control, UseFormSetValue} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

export interface IntegrationInstanceConfigurationDialogWorkflowListItemProps {
    control: Control<IntegrationInstanceConfigurationModel>;
    formState: FormState<IntegrationInstanceConfigurationModel>;
    label: string;
    setValue: UseFormSetValue<IntegrationInstanceConfigurationModel>;
    switchHidden?: boolean;
    workflow: WorkflowModel;
    workflowIndex: number;
}

const IntegrationInstanceConfigurationDialogWorkflowsStepItem = ({
    control,
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

    const workflowConnections: WorkflowConnectionModel[] = (workflow?.tasks ?? [])
        .flatMap((task) => task.connections ?? [])
        .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []));

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
                        className={twMerge(
                            'cursor-pointer rounded-full border-2 border-transparent bg-gray-200 transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2',
                            workflowEnabledMap.get(workflow.id!) && 'bg-blue-600'
                        )}
                        onClick={() => {
                            setValue(
                                `integrationInstanceConfigurationWorkflows.${workflowIndex!}.enabled`,
                                !workflowEnabledMap.get(workflow.id!)
                            );
                            setWorkflowEnabled(workflow.id!, !workflowEnabledMap.get(workflow.id!));
                        }}
                    >
                        <span
                            aria-hidden="true"
                            className={twMerge(
                                'pointer-events-none inline-block size-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                                workflowEnabledMap.get(workflow.id!) ? 'translate-x-5' : 'translate-x-0'
                            )}
                        />
                    </Switch>
                </div>
            )}

            {(workflowEnabledMap.get(workflow.id!) || switchHidden) && (
                <div className="mt-2 space-y-6">
                    {workflow && (
                        <div className="flex flex-col gap-3">
                            <Label className="font-semibold">Inputs</Label>

                            <IntegrationInstanceConfigurationDialogWorkflowsStepItemInputs
                                control={control}
                                formState={formState}
                                workflow={workflow}
                                workflowIndex={workflowIndex}
                            />
                        </div>
                    )}

                    <div className="flex flex-col gap-3">
                        <Label className="font-semibold">Connections</Label>

                        <IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections
                            control={control}
                            workflowConnections={workflowConnections}
                            workflowIndex={workflowIndex}
                        />
                    </div>
                </div>
            )}
        </div>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStepItem;
