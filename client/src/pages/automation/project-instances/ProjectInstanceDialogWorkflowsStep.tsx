import {Switch} from '@/components/ui/switch';
import {useWorkflowsEnabledStateStore} from '@/pages/automation/project-instances/stores/useWorkflowsEnabledStateStore';
import {PropertyType} from '@/types/projectTypes';
import Button from 'components/Button/Button';
import Input from 'components/Input/Input';
import Properties from 'components/Properties/Properties';
import {
    ProjectInstanceModel,
    WorkflowConnectionModel,
    WorkflowModel,
} from 'middleware/helios/configuration';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import {useState} from 'react';
import {UseFormGetValues, UseFormRegister} from 'react-hook-form';
import {FormState} from 'react-hook-form/dist/types/form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface ProjectInstanceDialogWorkflowListItemProps {
    formState: FormState<ProjectInstanceModel>;
    label: string;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    register: UseFormRegister<any>;
    switchHidden?: boolean;
    workflow: WorkflowModel;
    workflowIndex: number;
}

export const ProjectInstanceDialogWorkflowListItem = ({
    formState,
    label,
    register,
    switchHidden = false,
    workflow,
    workflowIndex,
}: ProjectInstanceDialogWorkflowListItemProps) => {
    const [selectedTabIndex, setSelectedTabIndex] = useState(0);
    const [setWorkflowEnabled, workflowEnabledMap] =
        useWorkflowsEnabledStateStore(
            useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [
                setWorkflowEnabled,
                workflowEnabledMap,
            ])
        );

    let connections: WorkflowConnectionModel[] = [];

    workflow.tasks?.forEach((task) => {
        if (task.connections) {
            connections = connections.concat(task.connections);
        }
    });

    workflow.triggers?.forEach((trigger) => {
        if (trigger.connections) {
            connections = connections.concat(trigger.connections);
        }
    });

    const tabs = [
        {
            content: workflow.inputs && (
                <Properties
                    path={`projectInstanceWorkflows.${workflowIndex!}.inputs`}
                    register={register}
                    formState={formState}
                    properties={workflow.inputs.map((input) => {
                        if (input.type === 'string') {
                            return {
                                controlType: 'TEXT',
                                type: 'STRING',
                                ...input,
                            } as PropertyType;
                        } else if (input.type === 'number') {
                            return {
                                type: 'NUMBER',
                                ...input,
                            } as PropertyType;
                        } else {
                            return {
                                controlType: 'SELECT',
                                type: 'BOOLEAN',
                                ...input,
                            } as PropertyType;
                        }
                    })}
                />
            ),
            name: 'Configuration',
        },
        {
            content: (
                <>
                    {register &&
                        connections?.map((connection, connectionIndex) => (
                            <Input
                                key={connection.componentName}
                                label={connection.componentName}
                                labelClassName="px-2"
                                // name={connection.componentName}
                                {...register(
                                    `projectInstanceWorkflows.${workflowIndex!}.connections.${connectionIndex}`
                                )}
                            />
                        ))}
                </>
            ),
            name: 'Connections',
        },
    ];

    return (
        <div>
            {register && (
                <input
                    type="hidden"
                    {...register(
                        `projectInstanceWorkflows.${workflowIndex!}.workflowId`,
                        {value: workflow.id}
                    )}
                />
            )}

            {!switchHidden && (
                <div className="flex cursor-pointer justify-between py-2">
                    <span className="font-semibold">{label}</span>

                    <Switch
                        checked={workflowEnabledMap.get(workflow.id!)}
                        className={twMerge(
                            'cursor-pointer rounded-full border-2 border-transparent bg-gray-200 transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-600 focus:ring-offset-2',
                            workflowEnabledMap.get(workflow.id!) &&
                                'bg-blue-600'
                        )}
                        onClick={() => {
                            setWorkflowEnabled(
                                workflow.id!,
                                !workflowEnabledMap.get(workflow.id!)
                            );
                        }}
                    >
                        <span
                            aria-hidden="true"
                            className={twMerge(
                                'pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out',
                                workflowEnabledMap.get(workflow.id!)
                                    ? 'translate-x-5'
                                    : 'translate-x-0'
                            )}
                        />
                    </Switch>
                </div>
            )}

            {workflowEnabledMap.get(workflow.id!) && register && (
                <input
                    type="hidden"
                    {...register(
                        `projectInstanceWorkflows.${workflowIndex!}.enabled`,
                        {value: workflowEnabledMap.get(workflow.id!)}
                    )}
                />
            )}

            {(workflowEnabledMap.get(workflow.id!) || switchHidden) && (
                <div className="mt-2">
                    {connections?.length ? (
                        <>
                            <nav aria-label="Tabs" className="flex">
                                {tabs.map((tab, index) => (
                                    <Button
                                        key={tab.name}
                                        label={tab.name}
                                        className={twMerge(
                                            'grow justify-center whitespace-nowrap rounded-none border-0 border-b-2 border-gray-200 bg-white px-3 py-2 text-sm font-medium text-gray-500 hover:border-blue-500 hover:text-blue-500 focus:border-blue-500 focus:text-blue-500 focus:outline-none',
                                            selectedTabIndex === index &&
                                                'border-blue-500 text-blue-500 hover:text-blue-500'
                                        )}
                                        onClick={() =>
                                            setSelectedTabIndex(index)
                                        }
                                    />
                                ))}
                            </nav>

                            {tabs[selectedTabIndex].content}
                        </>
                    ) : (
                        <>{tabs[0].content}</>
                    )}
                </div>
            )}
        </div>
    );
};

const ProjectInstanceDialogWorkflowsStep = ({
    formState,
    getValues,
    register,
}: {
    formState: FormState<ProjectInstanceModel>;
    getValues: UseFormGetValues<ProjectInstanceModel>;
    register: UseFormRegister<ProjectInstanceModel>;
}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(
        getValues().projectId!
    );

    return (
        <ul className="space-y-4">
            {workflows?.map((workflow, workflowIndex) => (
                <ProjectInstanceDialogWorkflowListItem
                    formState={formState}
                    key={workflow.id!}
                    workflow={workflow}
                    label={workflow.label!}
                    register={register}
                    workflowIndex={workflowIndex}
                />
            ))}
        </ul>
    );
};

export default ProjectInstanceDialogWorkflowsStep;
