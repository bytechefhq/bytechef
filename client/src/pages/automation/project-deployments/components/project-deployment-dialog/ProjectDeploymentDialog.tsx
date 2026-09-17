import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogNextButton,
    DialogPreviousButton,
    DialogSidebar,
    type DialogStepI,
    DialogStepIndicator,
    DialogSteps,
    DialogStepsProvider,
    DialogTrigger,
    useDialogSteps,
} from '@/components/Dialog';
import LoadingDots from '@/components/LoadingDots';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
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
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {ProjectDeploymentTagKeys} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {synchronizeGroupedConnections} from '@/shared/util/synchronizeGroupedConnections';
import {useQueryClient} from '@tanstack/react-query';
import {InfoIcon, RocketIcon} from 'lucide-react';
import {ReactNode, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useNavigate} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

import ProjectDeploymentDialogBasicStep from './ProjectDeploymentDialogBasicStep';
import ProjectDeploymentDialogWorkflowsStep from './ProjectDeploymentDialogWorkflowsStep';
import getWorkflowComponentConnections, {buildDeploymentWorkflows} from './projectDeploymentDialog-utils';

interface ProjectDeploymentDialogStepContentProps {
    basicStepContent: ReactNode;
    isWorkflowsPending: boolean;
    workflowsStepContent: ReactNode;
}

interface ProjectDeploymentDialogFooterProps {
    connectionsGrouped: boolean;
    hasEnabledWorkflows: boolean;
    hasVisibleConnections?: boolean;
    isDeploymentPending: boolean;
    onConnectionsGroupedChange: (grouped: boolean) => void;
}

const ProjectDeploymentDialogStepContent = ({
    basicStepContent,
    isWorkflowsPending,
    workflowsStepContent,
}: ProjectDeploymentDialogStepContentProps) => {
    const {currentStep} = useDialogSteps();

    if (currentStep.id !== 'workflows') {
        return <>{basicStepContent}</>;
    }

    if (isWorkflowsPending) {
        return (
            <div className="flex justify-center py-12">
                <LoadingDots />
            </div>
        );
    }

    return <>{workflowsStepContent}</>;
};

ProjectDeploymentDialogStepContent.displayName = 'ProjectDeploymentDialogStepContent';

const ProjectDeploymentDialogFooter = ({
    connectionsGrouped,
    hasEnabledWorkflows,
    hasVisibleConnections,
    isDeploymentPending,
    onConnectionsGroupedChange,
}: ProjectDeploymentDialogFooterProps) => {
    const {isLastStep} = useDialogSteps();

    return (
        <DialogFooter
            startContent={
                <>
                    <DialogCancelButton />

                    {isLastStep && hasVisibleConnections && (
                        <div className="ml-2 flex items-center gap-2">
                            <Switch
                                checked={connectionsGrouped}
                                label="Group Connections"
                                onCheckedChange={onConnectionsGroupedChange}
                            />

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <InfoIcon className="size-4 cursor-default text-content-neutral-secondary" />
                                </TooltipTrigger>

                                <TooltipContent>Connections grouped by their app.</TooltipContent>
                            </Tooltip>
                        </div>
                    )}
                </>
            }
        >
            <DialogPreviousButton className="lg:hidden" />

            {isLastStep && !hasEnabledWorkflows ? (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <span className="inline-flex">
                            <DialogNextButton
                                isPending={isDeploymentPending}
                                label="Next"
                                lastStepLabel={isDeploymentPending ? 'Saving...' : 'Save'}
                            />
                        </span>
                    </TooltipTrigger>

                    <TooltipContent>Enable at least one workflow to save this deployment</TooltipContent>
                </Tooltip>
            ) : (
                <DialogNextButton
                    isPending={isDeploymentPending}
                    label="Next"
                    lastStepLabel={isDeploymentPending ? 'Saving...' : 'Save'}
                />
            )}
        </DialogFooter>
    );
};

ProjectDeploymentDialogFooter.displayName = 'ProjectDeploymentDialogFooter';

interface ProjectDeploymentDialogProps {
    changeProjectVersion?: boolean;
    environmentEditable?: boolean;
    filterWorkflowUuids?: string[];
    onClose?: () => void;
    onOpenChange?: (isOpen: boolean) => void;
    onSuccess?: (deploymentId: number) => void;
    projectDeployment?: ProjectDeployment;
    projectDeployments?: ProjectDeployment[];
    projectDeploymentsLoading?: boolean;
    redirectOnSubmit?: boolean;
    showTabs?: boolean;
    triggerNode?: ReactNode;
}

const ProjectDeploymentDialog = ({
    changeProjectVersion = false,
    environmentEditable = false,
    filterWorkflowUuids,
    onClose,
    onOpenChange,
    onSuccess,
    projectDeployment,
    projectDeployments,
    projectDeploymentsLoading,
    redirectOnSubmit = true,
    showTabs,
    triggerNode,
}: ProjectDeploymentDialogProps) => {
    const [basicStepTab, setBasicStepTab] = useState<'new-deployment' | 'change-version'>(
        changeProjectVersion ? 'change-version' : 'new-deployment'
    );
    const [connectionsGrouped, setConnectionsGrouped] = useState(false);
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [selectedExistingDeployment, setSelectedExistingDeployment] = useState<ProjectDeployment | undefined>();
    const [tabInitialized, setTabInitialized] = useState(false);

    const effectiveProjectDeployment = selectedExistingDeployment ?? projectDeployment;
    const effectiveChangeProjectVersion = changeProjectVersion || !!selectedExistingDeployment;

    const initializedProjectKeyRef = useRef<string>('');

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setCurrentEnvironmentId = useEnvironmentStore((state) => state.setCurrentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const [resetWorkflowsEnabledStore, setWorkflowEnabled, workflowEnabledMap] = useWorkflowsEnabledStore(
        useShallow(({reset, setWorkflowEnabled, workflowEnabledMap}) => [reset, setWorkflowEnabled, workflowEnabledMap])
    );

    const {captureProjectDeploymentCreated} = useAnalytics();

    const form = useForm<ProjectDeployment>({
        defaultValues: {
            description: projectDeployment?.description || undefined,
            enabled: projectDeployment?.enabled || false,
            environmentId: projectDeployment?.environmentId ?? currentEnvironmentId,
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

    const watchedEnvironmentId = watch('environmentId');

    const deploymentEnvironmentId = watchedEnvironmentId ?? currentEnvironmentId;

    const {data: connections} = useGetWorkspaceConnectionsQuery(
        {
            environmentId: deploymentEnvironmentId,
            id: currentWorkspaceId!,
        },
        !!currentWorkspaceId
    );

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

    const onSuccessHandler = (deploymentId: number | void) => {
        if (!effectiveProjectDeployment?.id) {
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

        if (effectiveProjectDeployment?.projectId) {
            queryClient.invalidateQueries({
                queryKey: [...ProjectKeys.projects, effectiveProjectDeployment.projectId],
            });
        }

        const submittedEnvironmentId = getValues().environmentId;
        const submittedProjectId = getValues().projectId;

        if (onSuccess && deploymentId) {
            onSuccess(deploymentId);
        }

        closeDialog();

        if (redirectOnSubmit) {
            if (submittedEnvironmentId != null) {
                setCurrentEnvironmentId(submittedEnvironmentId);
            }

            const search = submittedProjectId != null ? `?projectId=${submittedProjectId}` : '';

            navigate(`/automation/deployments${search}`);
        }
    };

    const createProjectDeploymentMutation = useCreateProjectDeploymentMutation({
        onSuccess: (deploymentId) => onSuccessHandler(deploymentId),
    });

    const updateProjectDeploymentMutation = useUpdateProjectDeploymentMutation({
        onSuccess: () => onSuccessHandler(),
    });

    const isDeploymentPending = createProjectDeploymentMutation.isPending || updateProjectDeploymentMutation.isPending;

    const isSaveDisabled = !hasEnabledWorkflows || isDeploymentPending;

    const handleBasicStepTabChange = (tab: 'new-deployment' | 'change-version') => {
        setTabInitialized(true);

        setBasicStepTab(tab);

        if (tab === 'new-deployment' && selectedExistingDeployment) {
            setSelectedExistingDeployment(undefined);

            reset({
                description: projectDeployment?.description || undefined,
                enabled: projectDeployment?.enabled || false,
                environmentId: projectDeployment?.environmentId ?? currentEnvironmentId,
                name: projectDeployment?.name || undefined,
                projectDeploymentWorkflows: [],
                projectId: projectDeployment?.projectId || undefined,
                projectVersion: projectDeployment?.projectVersion || undefined,
                tags:
                    projectDeployment?.tags?.map((tag) => ({
                        ...tag,
                        label: tag.name,
                    })) || [],
            });

            initializedProjectKeyRef.current = '';
        }
    };

    const handleExistingDeploymentSelect = (deployment: ProjectDeployment) => {
        setSelectedExistingDeployment(deployment);

        reset({
            ...form.getValues(),
            description: deployment.description,
            enabled: deployment.enabled ?? false,
            environmentId: deployment.environmentId,
            name: deployment.name,
            projectDeploymentWorkflows: [],
            projectId: deployment.projectId,
            projectVersion: deployment.projectVersion,
            tags:
                deployment.tags?.map((tag) => ({
                    ...tag,
                    label: tag.name,
                })) ?? [],
        });

        initializedProjectKeyRef.current = '';
    };

    const basicStepContent =
        showTabs && !tabInitialized ? null : (
            <ProjectDeploymentDialogBasicStep
                basicStepTab={basicStepTab}
                changeProjectVersion={effectiveChangeProjectVersion}
                control={control}
                environmentEditable={environmentEditable}
                getValues={getValues}
                handleTabChange={handleBasicStepTabChange}
                onDeploymentSelect={handleExistingDeploymentSelect}
                projectDeployment={effectiveProjectDeployment}
                projectDeployments={projectDeployments}
                projectDeploymentsLoading={projectDeploymentsLoading}
                setValue={setValue}
                showTabs={showTabs}
            />
        );

    const workflowsStepContent = workflows && (
        <ProjectDeploymentDialogWorkflowsStep
            connections={connections}
            connectionsGrouped={connectionsGrouped}
            control={control}
            formState={formState}
            setValue={setValue}
            workflows={workflows}
        />
    );

    const isBasicStepBlocked =
        basicStepTab === 'change-version' &&
        !projectDeploymentsLoading &&
        (projectDeployments?.length ?? 0) === 0 &&
        !!showTabs;

    const steps = useMemo<DialogStepI[]>(
        () => [
            {canProceed: !isBasicStepBlocked, id: 'basic', label: 'Basic'},
            {canProceed: !isSaveDisabled, id: 'workflows', label: 'Workflows'},
        ],
        [isBasicStepBlocked, isSaveDisabled]
    );

    const closeDialog = () => {
        setIsOpen(false);

        setTimeout(() => {
            reset();

            initializedProjectKeyRef.current = '';

            setTabInitialized(false);

            setSelectedExistingDeployment(undefined);

            setBasicStepTab(changeProjectVersion ? 'change-version' : 'new-deployment');

            if (onClose) {
                onClose();
            }

            resetWorkflowsEnabledStore();

            setConnectionsGrouped(false);
        }, 300);
    };

    const validateStep = useCallback(
        (step: DialogStepI) => (step.id === 'basic' ? form.trigger(undefined, {shouldFocus: true}) : true),
        [form]
    );

    const handleConnectionsGroupedChange = (grouped: boolean) => {
        setConnectionsGrouped(grouped);

        if (!grouped || !workflows) {
            return;
        }

        const projectDeploymentWorkflows = getValues('projectDeploymentWorkflows') ?? [];

        workflows.forEach((workflow) => {
            const workflowIndex = projectDeploymentWorkflows.findIndex(
                (projectDeploymentWorkflow) => projectDeploymentWorkflow.workflowId === workflow.id
            );

            if (workflowIndex < 0) {
                return;
            }

            const workflowConnections = projectDeploymentWorkflows[workflowIndex]?.connections ?? [];

            synchronizeGroupedConnections({
                componentConnections: getWorkflowComponentConnections(workflow),
                getConnectionId: (index) => workflowConnections[index]?.connectionId,
                setConnectionId: (index, connectionId) =>
                    setValue(
                        `projectDeploymentWorkflows.${workflowIndex}.connections.${index}.connectionId`,
                        connectionId as number,
                        {shouldDirty: true}
                    ),
            });
        });
    };

    const handleSaveClick = useCallback(
        (formData: ProjectDeployment) => {
            if (!formData) {
                return;
            }

            const projectDeploymentWorkflows = buildDeploymentWorkflows(
                formData.projectDeploymentWorkflows,
                workflows ?? []
            );

            if (effectiveProjectDeployment?.id) {
                updateProjectDeploymentMutation.mutate({
                    ...effectiveProjectDeployment,
                    ...formData,
                    projectDeploymentWorkflows,
                } as ProjectDeployment);
            } else {
                createProjectDeploymentMutation.mutate({
                    ...formData,
                    environmentId: formData.environmentId ?? currentEnvironmentId,
                    projectDeploymentWorkflows,
                });
            }
        },
        [
            createProjectDeploymentMutation,
            currentEnvironmentId,
            effectiveProjectDeployment,
            updateProjectDeploymentMutation,
            workflows,
        ]
    );

    const handleCompleteClick = useCallback(() => handleSubmit(handleSaveClick)(), [handleSaveClick, handleSubmit]);

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
            const projectDeploymentWorkflow = effectiveProjectDeployment?.projectDeploymentWorkflows?.find(
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

    useLayoutEffect(() => {
        if (!isOpen) {
            setTabInitialized(false);

            return;
        }

        if (tabInitialized) {
            return;
        }

        if (changeProjectVersion) {
            setBasicStepTab('change-version');

            setTabInitialized(true);

            return;
        }

        if (projectDeploymentsLoading || projectDeployments === undefined) {
            return;
        }

        setBasicStepTab(projectDeployments.length > 0 ? 'change-version' : 'new-deployment');

        setTabInitialized(true);
    }, [isOpen, projectDeploymentsLoading, projectDeployments, changeProjectVersion, tabInitialized]);

    const isWizard = !effectiveProjectDeployment?.id || effectiveChangeProjectVersion;

    const wizardTitle = effectiveChangeProjectVersion ? 'Change Project Version' : 'New Deployment';

    const wizardDescription = effectiveChangeProjectVersion
        ? 'Deploy a different version of this project.'
        : 'Deploy a project version.';

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }

                if (onOpenChange) {
                    onOpenChange(isOpen);
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent
                hasSidebar={isWizard}
                onClick={(event) => event.stopPropagation()}
                onInteractOutside={(event) => event.preventDefault()}
            >
                {isWizard ? (
                    <DialogStepsProvider onComplete={handleCompleteClick} steps={steps} validateStep={validateStep}>
                        <DialogSidebar description={wizardDescription} icon={<RocketIcon />} title={wizardTitle}>
                            <DialogSteps />

                            <DialogStepIndicator />
                        </DialogSidebar>

                        <DialogMain>
                            <DialogHeader dialogDescription={wizardDescription} dialogTitle={wizardTitle} />

                            <DialogBody>
                                <WorkflowMockProvider>
                                    <Form {...form}>
                                        <ProjectDeploymentDialogStepContent
                                            basicStepContent={basicStepContent}
                                            isWorkflowsPending={isWorkflowsPending}
                                            workflowsStepContent={workflowsStepContent}
                                        />
                                    </Form>
                                </WorkflowMockProvider>
                            </DialogBody>

                            <ProjectDeploymentDialogFooter
                                connectionsGrouped={connectionsGrouped}
                                hasEnabledWorkflows={hasEnabledWorkflows}
                                hasVisibleConnections={hasVisibleConnections}
                                isDeploymentPending={isDeploymentPending}
                                onConnectionsGroupedChange={handleConnectionsGroupedChange}
                            />
                        </DialogMain>
                    </DialogStepsProvider>
                ) : (
                    <DialogMain>
                        <DialogHeader
                            description="Update the deployment name, description and tags."
                            title={`Edit Deployment - ${effectiveProjectDeployment?.name}`}
                        />

                        <DialogBody>
                            <WorkflowMockProvider>
                                <Form {...form}>{basicStepContent}</Form>
                            </WorkflowMockProvider>
                        </DialogBody>

                        <DialogFooter>
                            <DialogCancelButton />

                            {hasEnabledWorkflows ? (
                                <Button
                                    disabled={isSaveDisabled}
                                    icon={isDeploymentPending ? <LoadingIcon /> : undefined}
                                    label={isDeploymentPending ? 'Saving...' : 'Save'}
                                    onClick={handleCompleteClick}
                                />
                            ) : (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span className="inline-flex">
                                            <Button
                                                disabled={isSaveDisabled}
                                                icon={isDeploymentPending ? <LoadingIcon /> : undefined}
                                                label={isDeploymentPending ? 'Saving...' : 'Save'}
                                                onClick={handleCompleteClick}
                                            />
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent>
                                        Enable at least one workflow to save this deployment
                                    </TooltipContent>
                                </Tooltip>
                            )}
                        </DialogFooter>
                    </DialogMain>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ProjectDeploymentDialog;
