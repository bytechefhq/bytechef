import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-instances/stores/useWorkflowsEnabledStore';
import {
    EnvironmentModel,
    ProjectInstanceModel,
    ProjectInstanceWorkflowConnectionModel,
    ProjectInstanceWorkflowModel,
    WorkflowConnectionModel,
} from '@/shared/middleware/automation/configuration';
import {
    useCreateProjectInstanceMutation,
    useUpdateProjectInstanceMutation,
} from '@/shared/mutations/automation/projectInstances.mutations';
import {ProjectInstanceTagKeys} from '@/shared/queries/automation/projectInstanceTags.queries';
import {ProjectInstanceKeys} from '@/shared/queries/automation/projectInstances.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import ProjectInstanceDialogBasicStep from './ProjectInstanceDialogBasicStep';
import ProjectInstanceDialogWorkflowsStep from './ProjectInstanceDialogWorkflowsStep';

interface ProjectInstanceDialogProps {
    onClose?: () => void;
    projectInstance?: ProjectInstanceModel;
    triggerNode?: ReactNode;
    updateProjectVersion?: boolean;
}

const ProjectInstanceDialog = ({
    onClose,
    projectInstance,
    triggerNode,
    updateProjectVersion = false,
}: ProjectInstanceDialogProps) => {
    const [activeStepIndex, setActiveStepIndex] = useState(0);
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const [resetWorkflowsEnabledStore, setWorkflowEnabled] = useWorkflowsEnabledStore(
        useShallow(({reset, setWorkflowEnabled}) => [reset, setWorkflowEnabled])
    );

    const form = useForm<ProjectInstanceModel>({
        defaultValues: {
            description: projectInstance?.description || undefined,
            enabled: projectInstance?.enabled || false,
            environment: projectInstance?.environment || EnvironmentModel.Test,
            name: projectInstance?.name || undefined,
            projectId: projectInstance?.project?.id || undefined,
            projectInstanceWorkflows: [],
            projectVersion: projectInstance?.projectVersion || undefined,
            tags:
                projectInstance?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        },
    });

    const {control, formState, getValues, handleSubmit, reset, setValue} = form;

    const {data: workflows} = useGetProjectVersionWorkflowsQuery(
        getValues().projectId!,
        getValues().projectVersion!,
        !!getValues().projectId && !!getValues().projectVersion
    );

    const queryClient = useQueryClient();

    const onSuccess = () => {
        queryClient.invalidateQueries({
            queryKey: ProjectInstanceKeys.projectInstances,
        });
        queryClient.invalidateQueries({
            queryKey: ProjectInstanceTagKeys.projectInstanceTags,
        });
        queryClient.invalidateQueries({
            queryKey: ProjectKeys.filteredProjects({}),
        });

        closeDialog();
        setActiveStepIndex(0);
    };

    const createProjectInstanceMutation = useCreateProjectInstanceMutation({
        onSuccess,
    });

    const updateProjectInstanceMutation = useUpdateProjectInstanceMutation({
        onSuccess,
    });

    const projectInstanceDialogSteps = [
        {
            content: (
                <ProjectInstanceDialogBasicStep
                    control={control}
                    getValues={getValues}
                    projectInstance={projectInstance}
                    setValue={setValue}
                    updateProjectVersion={updateProjectVersion}
                />
            ),
            name: 'Basic',
        },
        {
            content: workflows && (
                <ProjectInstanceDialogWorkflowsStep
                    control={control}
                    formState={formState}
                    setValue={setValue}
                    workflows={workflows}
                />
            ),
            name: 'Workflows',
        },
    ];

    const closeDialog = () => {
        setIsOpen(false);

        setTimeout(() => {
            reset({
                environment: EnvironmentModel.Test,
                projectInstanceWorkflows: [],
            });

            setActiveStepIndex(0);

            if (onClose) {
                onClose();
            }

            resetWorkflowsEnabledStore();
        }, 300);
    };

    const handleNextClick = () => {
        setActiveStepIndex(activeStepIndex + 1);
    };

    const handleSaveClick = (formData: ProjectInstanceModel) => {
        if (!formData) {
            return;
        }

        if (projectInstance?.id) {
            updateProjectInstanceMutation.mutate({
                ...projectInstance,
                ...formData,
                projectInstanceWorkflows: formData.projectInstanceWorkflows?.map((projectInstanceWorkflow) => {
                    return {
                        ...projectInstanceWorkflow,
                        connections: projectInstanceWorkflow.enabled ? projectInstanceWorkflow.connections : [],
                        inputs: projectInstanceWorkflow.enabled ? projectInstanceWorkflow.inputs : {},
                    };
                }),
            } as ProjectInstanceModel);
        } else {
            createProjectInstanceMutation.mutate({
                ...formData,
                projectInstanceWorkflows: formData.projectInstanceWorkflows?.map((projectInstanceWorkflow) => {
                    return {
                        ...projectInstanceWorkflow,
                        connections: projectInstanceWorkflow.enabled ? projectInstanceWorkflow.connections : [],
                        inputs: projectInstanceWorkflow.enabled ? projectInstanceWorkflow.inputs : {},
                    };
                }),
            });
        }
    };

    useEffect(() => {
        if (workflows) {
            let projectInstanceWorkflows: ProjectInstanceWorkflowModel[] = [];

            for (let i = 0; i < workflows.length; i++) {
                const workflow = workflows[i];

                const projectInstanceWorkflow = projectInstance?.projectInstanceWorkflows?.find(
                    (projectInstanceWorkflow) =>
                        projectInstanceWorkflow.workflowReferenceCode === workflow.workflowReferenceCode
                );

                if (projectInstanceWorkflow && projectInstanceWorkflow.enabled) {
                    setWorkflowEnabled(workflow.id!, true);
                } else {
                    setWorkflowEnabled(workflow.id!, false);
                }

                let newProjectInstanceWorkflowConnections: ProjectInstanceWorkflowConnectionModel[] = [];

                const workflowConnections: WorkflowConnectionModel[] = (workflow?.tasks ?? [])
                    .flatMap((task) => task.connections ?? [])
                    .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []));

                for (const workflowConnection of workflowConnections) {
                    const projectInstanceWorkflowConnection = projectInstanceWorkflow?.connections?.find(
                        (projectInstanceWorkflowConnection) =>
                            projectInstanceWorkflowConnection.workflowNodeName ===
                                workflowConnection.workflowNodeName &&
                            projectInstanceWorkflowConnection.key === workflowConnection.key
                    );

                    newProjectInstanceWorkflowConnections = [
                        ...newProjectInstanceWorkflowConnections,
                        projectInstanceWorkflowConnection ??
                            ({
                                key: workflowConnection.key,
                                workflowNodeName: workflowConnection.workflowNodeName,
                            } as ProjectInstanceWorkflowConnectionModel),
                    ];
                }

                projectInstanceWorkflows = [
                    ...projectInstanceWorkflows,
                    {
                        ...(projectInstanceWorkflow ?? {}),
                        connections: newProjectInstanceWorkflowConnections,
                        version: undefined,
                        workflowId: workflow.id!,
                    },
                ];
            }

            setValue('projectInstanceWorkflows', projectInstanceWorkflows);
        }

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [getValues().projectId, getValues().projectVersion, workflows]);

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent className={twMerge('flex flex-col')} onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <DialogHeader>
                        <DialogTitle>
                            {updateProjectVersion
                                ? 'Upgrade Project Version'
                                : `${projectInstance?.id ? 'Edit' : 'New'} Instance ${!projectInstance?.id ? '-' : ''} ${
                                      !projectInstance?.id ? projectInstanceDialogSteps[activeStepIndex].name : ''
                                  }`}
                        </DialogTitle>

                        {!projectInstance?.id && (
                            <nav aria-label="Progress">
                                <ol className="space-y-4 md:flex md:space-y-0" role="list">
                                    {projectInstanceDialogSteps.map((step, index) => (
                                        <li className="md:flex-1" key={step.name}>
                                            <div
                                                className={twMerge(
                                                    'group flex flex-col border-l-4 py-2 pl-4 md:border-l-0 md:border-t-4 md:pb-0 md:pl-0 md:pt-4',
                                                    index <= activeStepIndex
                                                        ? 'border-gray-900 hover:border-gray-800'
                                                        : 'border-gray-200 hover:border-gray-30'
                                                )}
                                            ></div>
                                        </li>
                                    ))}
                                </ol>
                            </nav>
                        )}
                    </DialogHeader>

                    <div className={twMerge(activeStepIndex === 1 && 'max-h-[600px] overflow-y-auto')}>
                        {projectInstanceDialogSteps[activeStepIndex].content}
                    </div>

                    <DialogFooter>
                        {activeStepIndex === 0 && (
                            <>
                                <DialogClose asChild>
                                    <Button variant="outline">Cancel</Button>
                                </DialogClose>

                                {(!projectInstance?.id || updateProjectVersion) && (
                                    <Button onClick={handleSubmit(handleNextClick)}>Next</Button>
                                )}
                            </>
                        )}

                        {(activeStepIndex === 1 || (projectInstance?.id && !updateProjectVersion)) && (
                            <>
                                {activeStepIndex === 1 && (
                                    <Button onClick={() => setActiveStepIndex(activeStepIndex - 1)} variant="outline">
                                        Previous
                                    </Button>
                                )}

                                <Button onClick={handleSubmit(handleSaveClick)}>Save</Button>
                            </>
                        )}
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectInstanceDialog;
