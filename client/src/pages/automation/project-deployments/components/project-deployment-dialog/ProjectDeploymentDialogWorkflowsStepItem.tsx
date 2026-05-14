import Switch from '@/components/Switch/Switch';
import {Label} from '@/components/ui/label';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import ProjectDeploymentDialogWorkflowsStepItemInputs from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialogWorkflowsStepItemInputs';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-deployments/stores/useWorkflowsEnabledStore';
import ConnectionConfigurationList from '@/shared/components/ConnectionConfigurationList';
import {Connection, ProjectDeployment, Workflow} from '@/shared/middleware/automation/configuration';
import {FileInputIcon, Link2Icon} from 'lucide-react';
import {Control, FieldValues, FormState, UseFormSetValue, useWatch} from 'react-hook-form';
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
        <div className="">
            {showWorkflowToggle && (
                <div className="flex justify-between py-2">
                    <Label
                        className="w-full cursor-pointer text-base font-semibold"
                        htmlFor={`projectDeploymentWorkflows.${workflowIndex}`}
                    >
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
                <Tabs defaultValue="connections">
                    <TabsList className="flex w-full">
                        <TabsTrigger className="flex w-full data-[state=active]:shadow-none" value="connections">
                            <Link2Icon className="mr-2 size-4" />

                            <span>Connections</span>

                            <span className="ml-1">({componentConnections.length})</span>
                        </TabsTrigger>

                        <TabsTrigger className="flex w-full gap-2 data-[state=active]:shadow-none" value="inputs">
                            <FileInputIcon className="mr-2 size-4" />

                            <span>Inputs</span>

                            <span className="ml-1">({workflow.inputs?.length})</span>
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent className="py-3" value="connections">
                        <ConnectionConfigurationList
                            componentConnections={componentConnections}
                            connections={connections ?? []}
                            connectionsGrouped={connectionsGrouped}
                            control={control as unknown as Control<FieldValues>}
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

                    <TabsContent className="py-3" value="inputs">
                        <ProjectDeploymentDialogWorkflowsStepItemInputs
                            control={control}
                            formState={formState}
                            workflow={workflow}
                            workflowIndex={workflowIndex}
                        />
                    </TabsContent>
                </Tabs>
            )}
        </div>
    );
};

export default ProjectDeploymentDialogWorkflowsStepItem;
