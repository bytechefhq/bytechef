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

import getWorkflowComponentConnections, {getWorkflowInputs} from './projectDeploymentDialog-utils';

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
    workflows: Workflow[];
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
    workflows,
}: ProjectDeploymentDialogWorkflowListItemProps) => {
    const [setWorkflowEnabled, workflowEnabledMap] = useWorkflowsEnabledStore(
        useShallow(({setWorkflowEnabled, workflowEnabledMap}) => [setWorkflowEnabled, workflowEnabledMap])
    );

    const componentConnections = getWorkflowComponentConnections(workflow, workflows);

    const workflowInputs = getWorkflowInputs(workflow, workflows);

    console.log('workflow: ', workflow);
    console.log('workflows: ', workflows);

    const subflowLabelMap = new Map<string, string>();

    for (const subflowWorkflow of workflows) {
        if (subflowWorkflow.workflowUuid && subflowWorkflow.label) {
            subflowLabelMap.set(subflowWorkflow.workflowUuid, subflowWorkflow.label);
        }
    }

    const watchedConnections = useWatch({
        control,
        name: `projectDeploymentWorkflows.${workflowIndex}.connections`,
    });

    const workflowEnabled = workflowEnabledMap.get(workflow.id!);

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
                        className={twMerge(
                            'flex w-full cursor-pointer items-center gap-2 text-base font-normal',
                            workflowEnabled && 'font-semibold'
                        )}
                        htmlFor={`projectDeploymentWorkflows.${workflowIndex}`}
                    >
                        <div
                            className={twMerge(
                                'rounded-lg bg-surface-neutral-tertiary p-2',
                                workflowEnabled && 'bg-surface-brand-secondary'
                            )}
                        >
                            <WorkflowIcon
                                className={twMerge(
                                    'size-4 text-content-neutral-secondary transition-colors',
                                    workflowEnabled && 'text-content-brand-primary'
                                )}
                            />
                        </div>

                        {label}
                    </Label>

                    <Switch
                        checked={workflowEnabled}
                        id={`projectDeploymentWorkflows.${workflowIndex}`}
                        onCheckedChange={(value) => {
                            setValue(`projectDeploymentWorkflows.${workflowIndex!}.enabled`, value);

                            setWorkflowEnabled(workflow.id!, value);
                        }}
                    />
                </div>
            )}

            {(workflowEnabled || !showWorkflowToggle) && (
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

                            <span className="ml-1">({workflowInputs.length})</span>
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent className="mt-0" tabIndex={-1} value="connections">
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
                            subflowLabelMap={subflowLabelMap}
                            workflow={workflow}
                        />
                    </TabsContent>

                    <TabsContent className="mt-0" tabIndex={-1} value="inputs">
                        <InputConfigurationList
                            control={control as unknown as Control<FieldValues>}
                            controlPath={`projectDeploymentWorkflows.${workflowIndex}.inputs`}
                            formState={formState as unknown as FormState<FieldValues>}
                            inputs={workflowInputs}
                            subflowLabelMap={subflowLabelMap}
                        />
                    </TabsContent>
                </Tabs>
            )}
        </div>
    );
};

export default ProjectDeploymentDialogWorkflowsStepItem;
