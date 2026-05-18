import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-deployments/stores/useWorkflowsEnabledStore';
import ConnectionConfigurationList from '@/shared/components/ConnectionConfigurationList';
import InputConfigurationList from '@/shared/components/InputConfigurationList';
import {Connection, ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {FileInputIcon, Link2Icon, WorkflowIcon} from 'lucide-react';
import {Control, FieldValues, FormState, UseFormSetValue, useWatch} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import getWorkflowComponentConnections from './projectDeploymentDialog-utils';

export interface ProjectDeploymentDialogWorkflowListItemProps {
    connections?: Connection[];
    connectionsGrouped?: boolean;
    control: Control<ProjectDeployment>;
    formState: FormState<ProjectDeployment>;
    label?: string;
    setValue: UseFormSetValue<ProjectDeployment>;
    showWorkflowToggle?: boolean;
    workflow: Workflow;
    workflowIndex: number;
}

const ProjectDeploymentDialogWorkflowsStepItem = ({
    connections,
    connectionsGrouped,
    control,
    formState,
    label,
    setValue,
    showWorkflowToggle = false,
    workflow,
    workflowIndex,
}: ProjectDeploymentDialogWorkflowListItemProps) => {
    const [setWorkflowEnabled, workflowEnabledMap] = useWorkflowsEnabledStore(
        useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [setWorkflowEnabled, workflowEnabledMap])
    );

    const componentConnections = getWorkflowComponentConnections(workflow);

    const watchedConnections = useWatch({
        control,
        name: `projectDeploymentWorkflows.${workflowIndex}.connections`,
    });

    return (
        <div
            className={twMerge(
                showWorkflowToggle &&
                    'flex flex-col gap-3 rounded-lg border border-stroke-neutral-secondary bg-surface-main p-2'
            )}
        >
            {showWorkflowToggle && (
                <div className="flex items-center rounded-lg">
                    <Label
                        className="flex w-full cursor-pointer items-center gap-2 text-base font-semibold"
                        htmlFor={`projectDeploymentWorkflows.${workflowIndex}`}
                    >
                        <div className="rounded-lg bg-surface-brand-secondary p-2">
                            <WorkflowIcon className="size-4 text-content-brand-primary" />
                        </div>

                        {label}
                    </Label>

                    <Switch
                        checked={workflowEnabledMap.get(workflow.id!)}
                        id={`projectDeploymentWorkflows.${workflowIndex}`}
                        onCheckedChange={(value) => {
                            setValue(`projectDeploymentWorkflows.${workflowIndex!}.enabled`, value);

                            setWorkflowEnabled(workflow.id!, value);
                        }}
                    />
                </div>
            )}

            {(workflowEnabledMap.get(workflow.id!) || !showWorkflowToggle) && (
                <Tabs className="flex flex-col gap-2.5" defaultValue="connections">
                    <TabsList className="flex w-full">
                        <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="connections">
                            <Link2Icon className="mr-2 size-4" />

                            <span>Connections</span>

                            <span className="ml-1">({componentConnections.length})</span>
                        </TabsTrigger>

                        <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="inputs">
                            <FileInputIcon className="mr-2 size-4" />

                            <span>Inputs</span>

                            <span className="ml-1">({workflow.inputs?.length})</span>
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent className="mt-0" value="connections">
                        <ConnectionConfigurationList
                            componentConnections={componentConnections}
                            connections={connections ?? []}
                            connectionsGrouped={connectionsGrouped}
                            control={control as unknown as Control<FieldValues>}
                            fieldNamePrefix={`projectDeploymentWorkflows.${workflowIndex}.connections`}
                            getCurrentConnectionId={(connectionIndex) =>
                                watchedConnections?.[connectionIndex]?.connectionId
                            }
                            handleConnectionIdChange={(connectionIndex, connectionId) =>
                                setValue(
                                    `projectDeploymentWorkflows.${workflowIndex}.connections.${connectionIndex}.connectionId`,
                                    connectionId
                                )
                            }
                            workflow={workflow}
                        />
                    </TabsContent>

                    <TabsContent className="mt-0" value="inputs">
                        <InputConfigurationList
                            control={control as unknown as Control<FieldValues>}
                            controlPath={`projectDeploymentWorkflows.${workflowIndex}.inputs`}
                            formState={formState as unknown as FormState<FieldValues>}
                            inputs={workflow.inputs}
                        />
                    </TabsContent>
                </Tabs>
            )}
        </div>
    );
};

export default ProjectDeploymentDialogWorkflowsStepItem;
