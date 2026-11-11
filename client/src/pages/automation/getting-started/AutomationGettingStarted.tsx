import GettingStartedHelpCard from '@/components/GettingStarted/GettingStartedHelpCard';
import GettingStartedNextSteps from '@/components/GettingStarted/GettingStartedNextSteps';
import StepDoneText from '@/components/GettingStarted/StepDoneText';
import SubStepList from '@/components/GettingStarted/SubStepList';
import JourneySteps, {JourneyStepI} from '@/components/JourneySteps/JourneySteps';
import PageLoader from '@/components/PageLoader';
import {BUILD_FIRST_WORKFLOW_DOCUMENTATION_URL} from '@/pages/automation/journey/automationJourneySteps';
import {getAutomationJourneyFromSignals} from '@/pages/automation/journey/getAutomationJourney';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useAutomationGettingStartedQuery} from '@/shared/middleware/graphql';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {Navigate} from 'react-router-dom';

import {getAutomationNextSteps} from './automationNextSteps';
import AutomationGettingStartedDismissButton from './components/AutomationGettingStartedDismissButton';
import AutomationSetupDialogs from './components/AutomationSetupDialogs';
import FirstRunStepContent from './components/FirstRunStepContent';
import {getAutomationGettingStartedSteps, hasFirstRun, hasReachedStage} from './getAutomationGettingStartedSteps';
import {
    WorkflowCreateModeType,
    getDeploySubSteps,
    getProjectSubSteps,
    getPublishSubSteps,
    getWorkflowSubSteps,
} from './getAutomationGettingStartedSubSteps';
import {
    getGuideWorkflowHref,
    getRememberedGuideProjectId,
    getRememberedGuideWorkflow,
    resolveGuideProjectId,
    resolveGuideWorkflow,
} from './guideWorkflowStorage';
import scopeStatusToGuide from './scopeStatusToGuide';

const FIRST_RUN_POLLING_INTERVAL = 3000;

const FETCH_QUOTE_URL = 'https://dummyjson.com/quotes/random';

const LOG_QUOTE_DATA_PILL = '${httpClient_1.quote}';

const COPY_VALUES: Record<string, string> = {
    fetch: FETCH_QUOTE_URL,
    log: LOG_QUOTE_DATA_PILL,
};

const INTRO = 'Build a workflow that fetches a random quote and logs it. No connections needed.';

const AutomationGettingStarted = () => {
    const [createdProjectId, setCreatedProjectId] = useState<number | undefined>();
    const [openDialog, setOpenDialog] = useState<'project' | 'workflow' | null>(null);
    const [stepExpandedOverrides, setStepExpandedOverrides] = useState<Record<string, boolean>>({});

    const completedRef = useRef(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const workspaceScopeStatus = usePermissionStore(
        (state) => state.workspaceScopeStates[currentWorkspaceId]?.[currentEnvironmentId]?.status
    );

    const canCreateProject = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_CREATE');

    const queryClient = useQueryClient();

    const {data: projects} = useGetWorkspaceProjectsQuery({id: currentWorkspaceId});

    const {data, error, isLoading} = useAutomationGettingStartedQuery(
        {workspaceId: `${currentWorkspaceId}`},
        {
            enabled: canCreateProject,
            refetchInterval: (query) =>
                !query.state.error &&
                !!query.state.data?.automationGettingStarted.published &&
                !hasFirstRun(query.state.data?.automationGettingStarted)
                    ? FIRST_RUN_POLLING_INTERVAL
                    : false,
        }
    );

    const guideWorkflow = resolveGuideWorkflow(getRememberedGuideWorkflow(), projects);

    const workflowProjectId =
        resolveGuideProjectId(getRememberedGuideProjectId(), guideWorkflow, projects) ?? createdProjectId;
    const workflowCreateMode: WorkflowCreateModeType = workflowProjectId !== undefined ? 'dialog' : 'none';

    const hasGuideProject = workflowProjectId !== undefined;
    const hasGuideWorkflow = guideWorkflow !== undefined;

    const status = useMemo(
        () => scopeStatusToGuide({hasGuideProject, hasGuideWorkflow, status: data?.automationGettingStarted}),
        [data?.automationGettingStarted, hasGuideProject, hasGuideWorkflow]
    );

    const journey = useMemo(() => (status ? getAutomationJourneyFromSignals(status) : undefined), [status]);

    const guideWorkflowHref = getGuideWorkflowHref(guideWorkflow);

    const nextSteps = getAutomationNextSteps(guideWorkflow?.projectId ?? workflowProjectId);

    const closeDialog = () => {
        setOpenDialog(null);

        void queryClient.invalidateQueries({queryKey: ['automationGettingStarted']});
    };

    const completed = !!status?.completed;
    const firstRunExists = hasFirstRun(status);

    const toggleStep = useCallback((stepId: string, expanded: boolean) => {
        setStepExpandedOverrides((previousOverrides) => ({...previousOverrides, [stepId]: !expanded}));
    }, []);

    const builtSteps = useMemo<JourneyStepI[]>(() => {
        if (!journey || !status) {
            return [];
        }

        const {firstRunState, stepStatuses} = getAutomationGettingStartedSteps(journey, status);

        const projectSubStepActions: Partial<Record<string, () => void>> = {create: () => setOpenDialog('project')};

        const workflowSubStepActions: Partial<Record<string, () => void>> =
            workflowProjectId !== undefined ? {create: () => setOpenDialog('workflow')} : {};

        const workflowSubSteps = getWorkflowSubSteps(journey, status, guideWorkflowHref, workflowCreateMode).map(
            (subStep) => ({
                ...subStep,
                copyValue: COPY_VALUES[subStep.id],
            })
        );

        const showFirstRun = firstRunState !== 'waiting' || hasReachedStage(journey.stage, 'live');

        const firstRun = status.firstRun;

        return [
            {
                action: stepStatuses.createProject === 'complete' && <StepDoneText text="Project created" />,
                content: (
                    <SubStepList
                        ariaLabel="Project setup"
                        subStepActions={projectSubStepActions}
                        subSteps={getProjectSubSteps(journey, status)}
                    />
                ),
                description: 'A project holds your workflows.',
                id: 'createProject',
                label: 'Create a project',
                status: stepStatuses.createProject,
            },
            {
                action: stepStatuses.buildWorkflow === 'complete' && <StepDoneText text="Workflow added" />,
                content: (
                    <div className="space-y-2">
                        <SubStepList
                            ariaLabel="Workflow setup"
                            subStepActions={workflowSubStepActions}
                            subSteps={workflowSubSteps}
                        />

                        <a
                            className="block text-sm font-medium text-content-brand-primary hover:underline"
                            href={BUILD_FIRST_WORKFLOW_DOCUMENTATION_URL}
                            rel="noopener noreferrer"
                            target="_blank"
                        >
                            Build First Workflow
                        </a>
                    </div>
                ),
                description: 'Fetch a random quote and log it.',
                id: 'buildWorkflow',
                label: 'Build and test a workflow',
                status: stepStatuses.buildWorkflow,
            },
            {
                action: stepStatuses.publish === 'complete' && <StepDoneText text="Version published" />,
                content: (
                    <SubStepList
                        ariaLabel="Publish setup"
                        subSteps={getPublishSubSteps(journey, guideWorkflowHref, status)}
                    />
                ),
                description: 'Publishing freezes a version you can deploy.',
                id: 'publish',
                label: 'Publish a version',
                status: stepStatuses.publish,
            },
            {
                content: (
                    <div className="space-y-3">
                        <SubStepList
                            ariaLabel="Deployment setup"
                            subSteps={getDeploySubSteps(journey, status, guideWorkflowHref)}
                        />

                        {showFirstRun && (
                            <FirstRunStepContent
                                firstRun={firstRun}
                                firstRunState={firstRunState}
                                lastFailedRun={status.lastFailedRun}
                            />
                        )}
                    </div>
                ),
                description: 'Deploy the version, enable it and watch it run.',
                id: 'deploy',
                label: 'Deploy and enable',
                status: stepStatuses.deploy,
            },
        ];
    }, [guideWorkflowHref, journey, status, workflowCreateMode, workflowProjectId]);

    const steps = useMemo<JourneyStepI[]>(
        () =>
            builtSteps.map((step, index) => {
                if (!step.content) {
                    return step;
                }

                const expanded =
                    stepExpandedOverrides[step.id] ?? (index === builtSteps.length - 1 || step.status !== 'complete');

                return {
                    ...step,
                    content: expanded && step.content,
                    expanded,
                    onToggle: () => toggleStep(step.id, expanded),
                };
            }),
        [builtSteps, stepExpandedOverrides, toggleStep]
    );

    useEffect(() => {
        completedRef.current = completed;
    }, [completed]);

    useEffect(
        () => () => {
            if (completedRef.current) {
                void queryClient.invalidateQueries({queryKey: ['automationGettingStartedCompleted']});
            }
        },
        [queryClient]
    );

    if (currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT) {
        return <Navigate replace to="/automation/deployments" />;
    }

    if (!canCreateProject && (workspaceScopeStatus === 'loaded' || workspaceScopeStatus === 'error')) {
        return <Navigate replace to="/automation/projects" />;
    }

    return (
        <PageLoader errors={[error]} loading={isLoading}>
            <LayoutContainer
                header={
                    <Header
                        centerTitle
                        description={INTRO}
                        position="main"
                        right={status && !completed && <AutomationGettingStartedDismissButton />}
                        title={firstRunExists ? "You're set up" : 'Getting Started'}
                    />
                }
                leftSidebarOpen={false}
            >
                <div className="mx-auto w-full max-w-3xl space-y-6 p-4">
                    <JourneySteps aria-label="Setup steps" orientation="vertical" steps={steps} />

                    <hr className="border-stroke-neutral-secondary" />

                    <GettingStartedNextSteps steps={nextSteps} />

                    <GettingStartedHelpCard title="Need help automating with ByteChef?" />
                </div>
            </LayoutContainer>

            <AutomationSetupDialogs
                onClose={closeDialog}
                onProjectCreated={setCreatedProjectId}
                openDialog={openDialog}
                workflowProjectId={workflowProjectId}
            />
        </PageLoader>
    );
};

export default AutomationGettingStarted;
