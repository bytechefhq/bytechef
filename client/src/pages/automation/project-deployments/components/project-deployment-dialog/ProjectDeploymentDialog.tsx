import LoadingIcon from '@/components/LoadingIcon';
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
import {useWorkflowsEnabledStore} from '@/pages/automation/project-deployments/stores/useWorkflowsEnabledStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {
    Environment,
    ProjectDeployment,
    ProjectDeploymentWorkflow,
    ProjectDeploymentWorkflowConnection,
    WorkflowConnection,
} from '@/shared/middleware/automation/configuration';
import {
    useCreateProjectDeploymentMutation,
    useUpdateProjectDeploymentMutation,
} from '@/shared/mutations/automation/projectDeployments.mutations';
import {ProjectDeploymentTagKeys} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import ProjectDeploymentDialogBasicStep from './ProjectDeploymentDialogBasicStep';
import ProjectDeploymentDialogWorkflowsStep from './ProjectDeploymentDialogWorkflowsStep';

interface ProjectDeploymentDialogProps {
    onClose?: () => void;
    projectDeployment?: ProjectDeployment;
    triggerNode?: ReactNode;
    updateProjectVersion?: boolean;
}

const ProjectDeploymentDialog = ({
    onClose,
    projectDeployment,
    triggerNode,
    updateProjectVersion = false,
}: ProjectDeploymentDialogProps) => {
    const [activeStepIndex, setActiveStepIndex] = useState(0);
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const {currentWorkspaceId} = useWorkspaceStore();
    const [resetWorkflowsEnabledStore, setWorkflowEnabled] = useWorkflowsEnabledStore(
        useShallow(({reset, setWorkflowEnabled}) => [reset, setWorkflowEnabled])
    );

    const {captureProjectDeploymentCreated} = useAnalytics();

    const form = useForm<ProjectDeployment>({
        defaultValues: {
            description: projectDeployment?.description || undefined,
            enabled: projectDeployment?.enabled || false,
            environment: projectDeployment?.environment || Environment.Test,
            name: projectDeployment?.name || undefined,
            projectDeploymentWorkflows: [],
            projectId: projectDeployment?.projectId || undefined,
            projectVersion: projectDeployment?.projectVersion || undefined,
            tags:
                projectDeployment?.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        },
    });

    const {control, formState, getValues, handleSubmit, reset, setValue} = form;

    const {data: workflows} = useGetProjectVersionWorkflowsQuery(
        getValues().projectId!,
        getValues().projectVersion!,
        true,
        !!getValues().projectId && !!getValues().projectVersion
    );

    const queryClient = useQueryClient();

    const onSuccess = () => {
        if (!projectDeployment?.id) {
            captureProjectDeploymentCreated();
        }

        queryClient.invalidateQueries({
            queryKey: ProjectDeploymentKeys.projectDeployments,
        });
        queryClient.invalidateQueries({
            queryKey: ProjectDeploymentTagKeys.projectDeploymentTags,
        });
        queryClient.invalidateQueries({
            queryKey: ProjectKeys.filteredProjects({id: currentWorkspaceId!}),
        });

        if (projectDeployment?.projectId) {
            queryClient.invalidateQueries({
                queryKey: [...ProjectKeys.projects, projectDeployment?.projectId],
            });
        }

        closeDialog();
        setActiveStepIndex(0);
    };

    const createProjectDeploymentMutation = useCreateProjectDeploymentMutation({
        onSuccess,
    });

    const updateProjectDeploymentMutation = useUpdateProjectDeploymentMutation({
        onSuccess,
    });

    const projectDeploymentDialogSteps = [
        {
            content: (
                <ProjectDeploymentDialogBasicStep
                    control={control}
                    getValues={getValues}
                    projectDeployment={projectDeployment}
                    setValue={setValue}
                    updateProjectVersion={updateProjectVersion}
                />
            ),
            name: 'Basic',
        },
        {
            content: workflows && (
                <ProjectDeploymentDialogWorkflowsStep
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
                environment: Environment.Test,
                projectDeploymentWorkflows: [],
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

    const handleSaveClick = (formData: ProjectDeployment) => {
        if (!formData) {
            return;
        }

        if (projectDeployment?.id) {
            updateProjectDeploymentMutation.mutate({
                ...projectDeployment,
                ...formData,
                projectDeploymentWorkflows: formData.projectDeploymentWorkflows?.map((projectDeploymentWorkflow) => {
                    return {
                        ...projectDeploymentWorkflow,
                        connections: projectDeploymentWorkflow.enabled ? projectDeploymentWorkflow.connections : [],
                        inputs: projectDeploymentWorkflow.enabled ? projectDeploymentWorkflow.inputs : {},
                    };
                }),
            } as ProjectDeployment);
        } else {
            createProjectDeploymentMutation.mutate({
                ...formData,
                projectDeploymentWorkflows: formData.projectDeploymentWorkflows?.map((projectDeploymentWorkflow) => {
                    return {
                        ...projectDeploymentWorkflow,
                        connections: projectDeploymentWorkflow.enabled ? projectDeploymentWorkflow.connections : [],
                        inputs: projectDeploymentWorkflow.enabled ? projectDeploymentWorkflow.inputs : {},
                    };
                }),
            });
        }
    };

    useEffect(() => {
        if (workflows) {
            let projectDeploymentWorkflows: ProjectDeploymentWorkflow[] = [];

            for (let i = 0; i < workflows.length; i++) {
                const workflow = workflows[i];

                const projectDeploymentWorkflow = projectDeployment?.projectDeploymentWorkflows?.find(
                    (projectDeploymentWorkflow) =>
                        projectDeploymentWorkflow.workflowReferenceCode === workflow.workflowReferenceCode
                );

                if (projectDeploymentWorkflow && projectDeploymentWorkflow.enabled) {
                    setWorkflowEnabled(workflow.id!, true);
                } else {
                    setWorkflowEnabled(workflow.id!, false);
                }

                let newProjectDeploymentWorkflowConnections: ProjectDeploymentWorkflowConnection[] = [];

                const workflowConnections: WorkflowConnection[] = (workflow?.tasks ?? [])
                    .flatMap((task) => task.connections ?? [])
                    .concat((workflow?.triggers ?? []).flatMap((trigger) => trigger.connections ?? []));

                for (const workflowConnection of workflowConnections) {
                    const projectDeploymentWorkflowConnection = projectDeploymentWorkflow?.connections?.find(
                        (projectDeploymentWorkflowConnection) =>
                            projectDeploymentWorkflowConnection.workflowNodeName ===
                                workflowConnection.workflowNodeName &&
                            projectDeploymentWorkflowConnection.key === workflowConnection.key
                    );

                    newProjectDeploymentWorkflowConnections = [
                        ...newProjectDeploymentWorkflowConnections,
                        projectDeploymentWorkflowConnection ??
                            ({
                                key: workflowConnection.key,
                                workflowNodeName: workflowConnection.workflowNodeName,
                            } as ProjectDeploymentWorkflowConnection),
                    ];
                }

                projectDeploymentWorkflows = [
                    ...projectDeploymentWorkflows,
                    {
                        ...(projectDeploymentWorkflow ?? {}),
                        connections: newProjectDeploymentWorkflowConnections,
                        version: undefined,
                        workflowId: workflow.id!,
                    },
                ];
            }

            setValue('projectDeploymentWorkflows', projectDeploymentWorkflows);
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
                                : `${projectDeployment?.id ? 'Edit' : 'New'} Deployment ${!projectDeployment?.id ? '-' : ''} ${
                                      !projectDeployment?.id ? projectDeploymentDialogSteps[activeStepIndex].name : ''
                                  }`}
                        </DialogTitle>

                        {!projectDeployment?.id && (
                            <nav aria-label="Progress">
                                <ol className="space-y-4 md:flex md:space-y-0" role="list">
                                    {projectDeploymentDialogSteps.map((step, index) => (
                                        <li className="md:flex-1" key={step.name}>
                                            <div
                                                className={twMerge(
                                                    'group flex flex-col border-l-4 py-2 pl-4 md:border-l-0 md:border-t-4 md:pb-0 md:pl-0 md:pt-4',
                                                    index <= activeStepIndex
                                                        ? 'border-gray-900 hover:border-gray-800'
                                                        : 'hover:border-gray-30 border-gray-200'
                                                )}
                                            ></div>
                                        </li>
                                    ))}
                                </ol>
                            </nav>
                        )}
                    </DialogHeader>

                    <div
                        className={twMerge(
                            activeStepIndex === 1 && 'max-h-project-deployment-dialog-height overflow-y-auto'
                        )}
                    >
                        {projectDeploymentDialogSteps[activeStepIndex].content}
                    </div>

                    <DialogFooter>
                        {activeStepIndex === 0 && (
                            <>
                                <DialogClose asChild>
                                    <Button variant="outline">Cancel</Button>
                                </DialogClose>

                                {(!projectDeployment?.id || updateProjectVersion) && (
                                    <Button onClick={handleSubmit(handleNextClick)}>Next</Button>
                                )}
                            </>
                        )}

                        {(activeStepIndex === 1 || (projectDeployment?.id && !updateProjectVersion)) && (
                            <>
                                {activeStepIndex === 1 && (
                                    <Button onClick={() => setActiveStepIndex(activeStepIndex - 1)} variant="outline">
                                        Previous
                                    </Button>
                                )}

                                <Button
                                    disabled={
                                        createProjectDeploymentMutation.isPending ||
                                        updateProjectDeploymentMutation.isPending
                                    }
                                    onClick={handleSubmit(handleSaveClick)}
                                >
                                    {createProjectDeploymentMutation.isPending ||
                                        (updateProjectDeploymentMutation.isPending && <LoadingIcon />)}
                                    Save
                                </Button>
                            </>
                        )}
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectDeploymentDialog;
