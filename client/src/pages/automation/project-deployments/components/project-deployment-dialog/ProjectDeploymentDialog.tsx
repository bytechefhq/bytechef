import Button from '@/components/Button/Button';
import LoadingDots from '@/components/LoadingDots';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {Form} from '@/components/ui/form';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useWorkflowsEnabledStore} from '@/pages/automation/project-deployments/stores/useWorkflowsEnabledStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {WorkflowMockProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {
    ProjectDeployment,
    ProjectDeploymentWorkflow,
    ProjectDeploymentWorkflowConnection,
} from '@/shared/middleware/automation/configuration';
import {
    useCreateProjectDeploymentMutation,
    useUpdateProjectDeploymentMutation,
} from '@/shared/mutations/automation/projectDeployments.mutations';
import {ProjectDeploymentTagKeys} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {InfoIcon} from 'lucide-react';
import {ReactNode, useEffect, useMemo, useRef, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useLocation, useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import ProjectDeploymentDialogBasicStep from './ProjectDeploymentDialogBasicStep';
import ProjectDeploymentDialogWorkflowsStep from './ProjectDeploymentDialogWorkflowsStep';
import getWorkflowComponentConnections from './projectDeploymentDialog-utils';

interface ProjectDeploymentDialogProps {
    changeProjectVersion?: boolean;
    filterWorkflowUuids?: string[];
    onClose?: () => void;
    projectDeployment?: ProjectDeployment;
    triggerNode?: ReactNode;
}

const ProjectDeploymentDialog = ({
    changeProjectVersion = false,
    filterWorkflowUuids,
    onClose,
    projectDeployment,
    triggerNode,
}: ProjectDeploymentDialogProps) => {
    const [activeStepIndex, setActiveStepIndex] = useState(0);
    const [groupConnections, setGroupConnections] = useState(false);
    const [isOpen, setIsOpen] = useState(!triggerNode);

    const initializedProjectKeyRef = useRef<string>('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const [resetWorkflowsEnabledStore, setWorkflowEnabled, workflowEnabledMap] = useWorkflowsEnabledStore(
        useShallow(({reset, setWorkflowEnabled, workflowEnabledMap}) => [reset, setWorkflowEnabled, workflowEnabledMap])
    );

    const {captureProjectDeploymentCreated} = useAnalytics();

    const form = useForm<ProjectDeployment>({
        defaultValues: {
            description: projectDeployment?.description || undefined,
            enabled: projectDeployment?.enabled || false,
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

    const {control, formState, getValues, handleSubmit, reset, setValue, watch} = form;

    const watchedProjectDeploymentWorkflows = watch('projectDeploymentWorkflows');
    const hasEnabledWorkflows = watchedProjectDeploymentWorkflows?.some((workflow) => workflow.enabled) ?? false;

    const {data: projectVersionWorkflows, isPending: isWorkflowsPending} = useGetProjectVersionWorkflowsQuery(
        getValues().projectId!,
        getValues().projectVersion!,
        true,
        !!getValues().projectId && !!getValues().projectVersion,
        false
    );

    const workflows = useMemo(() => {
        if (!projectVersionWorkflows) {
            return undefined;
        }

        if (filterWorkflowUuids === undefined) {
            return projectVersionWorkflows;
        }

        if (filterWorkflowUuids.length === 0) {
            return [];
        }

        return projectVersionWorkflows.filter((workflow) => filterWorkflowUuids.includes(workflow.workflowUuid!));
    }, [projectVersionWorkflows, filterWorkflowUuids]);

    const hasVisibleConnections = workflows?.some((workflow) => {
        const isEnabled = workflowEnabledMap.get(workflow.id!);

        if (!isEnabled) {
            return false;
        }

        return getWorkflowComponentConnections(workflow).length > 0;
    });

    const queryClient = useQueryClient();

    const navigate = useNavigate();
    const location = useLocation();

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

        if (
            !location.pathname.includes('deployments') &&
            !location.pathname.includes('api-platform') &&
            !location.pathname.includes('mcp')
        ) {
            navigate('/automation/deployments');
        }
    };

    const createProjectDeploymentMutation = useCreateProjectDeploymentMutation({
        onSuccess,
    });

    const updateProjectDeploymentMutation = useUpdateProjectDeploymentMutation({
        onSuccess,
    });

    const isDeploymentPending = createProjectDeploymentMutation.isPending || updateProjectDeploymentMutation.isPending;

    const isSaveDisabled = !hasEnabledWorkflows || isDeploymentPending;

    const projectDeploymentDialogSteps = [
        {
            content: (
                <ProjectDeploymentDialogBasicStep
                    changeProjectVersion={changeProjectVersion}
                    control={control}
                    getValues={getValues}
                    projectDeployment={projectDeployment}
                    setValue={setValue}
                />
            ),
            name: 'Basic',
        },
        {
            content: workflows && (
                <ProjectDeploymentDialogWorkflowsStep
                    control={control}
                    formState={formState}
                    groupConnections={groupConnections}
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
            reset();

            setActiveStepIndex(0);

            initializedProjectKeyRef.current = '';

            if (onClose) {
                onClose();
            }

            resetWorkflowsEnabledStore();

            setGroupConnections(false);
        }, 300);
    };

    const handleNextClick = () => setActiveStepIndex(activeStepIndex + 1);

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
                        connections: projectDeploymentWorkflow.enabled
                            ? projectDeploymentWorkflow.connections?.filter((connection) => connection.connectionId)
                            : [],
                        inputs: projectDeploymentWorkflow.enabled ? projectDeploymentWorkflow.inputs : {},
                    };
                }),
            } as ProjectDeployment);
        } else {
            createProjectDeploymentMutation.mutate({
                ...formData,
                environmentId: currentEnvironmentId,
                projectDeploymentWorkflows: formData.projectDeploymentWorkflows?.map((projectDeploymentWorkflow) => {
                    return {
                        ...projectDeploymentWorkflow,
                        connections: projectDeploymentWorkflow.enabled
                            ? projectDeploymentWorkflow.connections?.filter((connection) => connection.connectionId)
                            : [],
                        inputs: projectDeploymentWorkflow.enabled ? projectDeploymentWorkflow.inputs : {},
                    };
                }),
            });
        }
    };

    useEffect(() => {
        if (!workflows?.length) {
            return;
        }

        const filterKey = filterWorkflowUuids ? filterWorkflowUuids.join(',') : '';

        const projectKey = `${getValues().projectId}-${getValues().projectVersion}-${filterKey}`;

        if (initializedProjectKeyRef.current === projectKey) {
            return;
        }

        initializedProjectKeyRef.current = projectKey;

        const projectDeploymentWorkflows: ProjectDeploymentWorkflow[] = workflows.map((workflow) => {
            const projectDeploymentWorkflow = projectDeployment?.projectDeploymentWorkflows?.find(
                (projectDeploymentWorkflow) => projectDeploymentWorkflow.workflowUuid === workflow.workflowUuid
            );

            setWorkflowEnabled(workflow.id!, !!(projectDeploymentWorkflow && projectDeploymentWorkflow.enabled));

            const componentConnections = getWorkflowComponentConnections(workflow);

            const newProjectDeploymentWorkflowConnections: ProjectDeploymentWorkflowConnection[] =
                componentConnections.map((componentConnection) => {
                    const existingConnection = projectDeploymentWorkflow?.connections?.find(
                        (projectDeploymentWorkflowConnection) =>
                            projectDeploymentWorkflowConnection.workflowNodeName ===
                                componentConnection.workflowNodeName &&
                            projectDeploymentWorkflowConnection.workflowConnectionKey === componentConnection.key
                    );

                    return (
                        existingConnection ??
                        ({
                            workflowConnectionKey: componentConnection.key,
                            workflowNodeName: componentConnection.workflowNodeName,
                        } as ProjectDeploymentWorkflowConnection)
                    );
                });

            return {
                ...(projectDeploymentWorkflow ?? {}),
                connections: newProjectDeploymentWorkflowConnections,
                version: undefined,
                workflowId: workflow.id!,
            };
        });

        setValue('projectDeploymentWorkflows', projectDeploymentWorkflows);

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

            <DialogContent className="flex flex-col gap-0 p-0" onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <DialogHeader className="flex flex-row items-center justify-between gap-1 space-y-0 p-6">
                        <div className="flex w-full flex-col space-y-1">
                            <DialogTitle>
                                {changeProjectVersion
                                    ? 'Change Project Version'
                                    : `${projectDeployment?.id ? 'Edit' : 'New'} Deployment ${!projectDeployment?.id ? '-' : ''} ${
                                          !projectDeployment?.id
                                              ? projectDeploymentDialogSteps[activeStepIndex].name
                                              : ''
                                      }`}
                            </DialogTitle>

                            {!projectDeployment?.id && (
                                <nav aria-label="Progress">
                                    <ol className="space-y-4 md:flex md:space-y-0" role="list">
                                        {projectDeploymentDialogSteps.map((step, index) => (
                                            <li className="md:flex-1" key={step.name}>
                                                <div
                                                    className={twMerge(
                                                        'group flex flex-col border-l-4 py-2 pl-4 md:border-l-0 md:border-t-4 md:pb-0 md:pl-0',
                                                        index <= activeStepIndex
                                                            ? 'border-gray-900 hover:border-gray-800'
                                                            : 'hover:border-gray-30 border-gray-200'
                                                    )}
                                                />
                                            </li>
                                        ))}
                                    </ol>
                                </nav>
                            )}
                        </div>

                        <DialogCloseButton />
                    </DialogHeader>

                    <WorkflowMockProvider>
                        <div
                            className={twMerge('px-6', activeStepIndex === 1 && 'max-h-dialog-height overflow-y-auto')}
                        >
                            {activeStepIndex === 1 && isWorkflowsPending ? (
                                <div className="flex justify-center py-12">
                                    <LoadingDots />
                                </div>
                            ) : (
                                projectDeploymentDialogSteps[activeStepIndex].content
                            )}
                        </div>
                    </WorkflowMockProvider>

                    <DialogFooter className="p-6">
                        {activeStepIndex === 0 && (
                            <>
                                <DialogClose asChild>
                                    <Button label="Cancel" variant="outline" />
                                </DialogClose>

                                {(!projectDeployment?.id || changeProjectVersion) && (
                                    <Button label="Next" onClick={handleSubmit(handleNextClick)} />
                                )}
                            </>
                        )}

                        {(activeStepIndex === 1 || (projectDeployment?.id && !changeProjectVersion)) && (
                            <>
                                {activeStepIndex === 1 && hasVisibleConnections && (
                                    <div className="mr-auto flex items-center gap-2">
                                        <Switch checked={groupConnections} onCheckedChange={setGroupConnections} />

                                        <span className="text-sm font-semibold">Group Connections</span>

                                        <Tooltip>
                                            <TooltipTrigger asChild>
                                                <InfoIcon className="size-4 cursor-default text-content-onsurface-secondary" />
                                            </TooltipTrigger>

                                            <TooltipContent>Connections grouped by their app.</TooltipContent>
                                        </Tooltip>
                                    </div>
                                )}

                                {activeStepIndex === 1 && (
                                    <Button
                                        label="Previous"
                                        onClick={() => setActiveStepIndex(activeStepIndex - 1)}
                                        variant="outline"
                                    />
                                )}

                                {!hasEnabledWorkflows ? (
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <span className="inline-flex">
                                                <Button
                                                    disabled={isSaveDisabled}
                                                    icon={isDeploymentPending ? <LoadingIcon /> : undefined}
                                                    label={isDeploymentPending ? 'Saving...' : 'Save'}
                                                    onClick={handleSubmit(handleSaveClick)}
                                                />
                                            </span>
                                        </TooltipTrigger>

                                        <TooltipContent>
                                            Enable at least one workflow to save this deployment
                                        </TooltipContent>
                                    </Tooltip>
                                ) : (
                                    <Button
                                        disabled={isSaveDisabled}
                                        icon={isDeploymentPending ? <LoadingIcon /> : undefined}
                                        label={isDeploymentPending ? 'Saving...' : 'Save'}
                                        onClick={handleSubmit(handleSaveClick)}
                                    />
                                )}
                            </>
                        )}
                    </DialogFooter>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectDeploymentDialog;
