import {Switch} from '@/components/ui/switch';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {IntegrationInstanceConfigurationModel} from '@/middleware/embedded/configuration';
import {WorkflowConnectionModel} from '@/middleware/platform/configuration';
import IntegrationInstanceConfigurationDialogWorkflowsStepItemConfiguration from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogWorkflowsStepItemConfiguration';
import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections from '@/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections';
import {useWorkflowsEnabledStore} from '@/pages/embedded/integration-instance-configurations/stores/useWorkflowsEnabledStore';
import {useGetWorkflowQuery} from '@/queries/embedded/workflows.queries';
import {Control, UseFormRegister, UseFormSetValue} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

export interface IntegrationInstanceConfigurationDialogWorkflowsStepItemProps {
    control: Control<IntegrationInstanceConfigurationModel>;
    formState: FormState<IntegrationInstanceConfigurationModel>;
    label: string;
    register: UseFormRegister<IntegrationInstanceConfigurationModel>;
    setValue: UseFormSetValue<IntegrationInstanceConfigurationModel>;
    switchHidden?: boolean;
    workflowId: string;
    workflowIndex: number;
}

const IntegrationInstanceConfigurationDialogWorkflowsStepItem = ({
    control,
    formState,
    label,
    register,
    setValue,
    switchHidden = false,
    workflowId,
    workflowIndex,
}: IntegrationInstanceConfigurationDialogWorkflowsStepItemProps) => {
    const [setWorkflowEnabled, workflowEnabled] = useWorkflowsEnabledStore(
        useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [setWorkflowEnabled, workflowEnabledMap])
    );

    const {data: workflow} = useGetWorkflowQuery(workflowId);

    const workflowConnections: WorkflowConnectionModel[] = (workflow?.tasks ?? [])
        .flatMap((task) => {
            return task.connections ?? [];
        })
        .concat(
            (workflow?.triggers ?? []).flatMap((trigger) => {
                return trigger.connections ?? [];
            })
        );

    return (
        <div>
            {register && (
                <input
                    type="hidden"
                    {...register(`integrationInstanceConfigurationWorkflows.${workflowIndex!}.workflowId`, {
                        value: workflowId,
                    })}
                />
            )}

            {!switchHidden && (
                <div className="flex cursor-pointer justify-between py-2">
                    <span className="font-semibold">{label}</span>

                    <Switch
                        checked={workflowEnabled.get(workflowId)}
                        className={twMerge(
                            'cursor-pointer rounded-full border-2 border-transparent bg-gray-200 transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2',
                            workflowEnabled.get(workflowId!) && 'bg-blue-600'
                        )}
                        onClick={() => {
                            setValue(
                                `integrationInstanceConfigurationWorkflows.${workflowIndex!}.enabled`,
                                !workflowEnabled.get(workflowId)
                            );
                            setWorkflowEnabled(workflowId, !workflowEnabled.get(workflowId));
                        }}
                    >
                        <span
                            aria-hidden="true"
                            className={twMerge(
                                'pointer-events-none inline-block size-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                                workflowEnabled.get(workflowId) ? 'translate-x-5' : 'translate-x-0'
                            )}
                        />
                    </Switch>
                </div>
            )}

            {(workflowEnabled.get(workflowId) || switchHidden) && (
                <div className="mt-2">
                    <Tabs aria-label="Tabs" defaultValue="configuration">
                        <TabsList className="grid w-full grid-cols-2">
                            <TabsTrigger value="configuration">Configuration</TabsTrigger>

                            <TabsTrigger value="connections">Connections</TabsTrigger>
                        </TabsList>

                        {workflow && (
                            <TabsContent value="configuration">
                                <IntegrationInstanceConfigurationDialogWorkflowsStepItemConfiguration
                                    control={control}
                                    formState={formState}
                                    workflow={workflow}
                                    workflowIndex={workflowIndex}
                                />
                            </TabsContent>
                        )}

                        <TabsContent className="grid gap-4" value="connections">
                            <IntegrationInstanceConfigurationDialogWorkflowsStepItemConnections
                                control={control}
                                workflowConnections={workflowConnections}
                                workflowIndex={workflowIndex}
                            />
                        </TabsContent>
                    </Tabs>
                </div>
            )}
        </div>
    );
};

export default IntegrationInstanceConfigurationDialogWorkflowsStepItem;
