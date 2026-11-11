import {AutomationGettingStartedStatusType} from '@/pages/automation/getting-started/automationGettingStartedStatus';
import {FirstRunStateType, hasFirstRun} from '@/pages/automation/getting-started/getAutomationGettingStartedSteps';
import {
    getRememberedGuideWorkflow,
    isEditorGuideCollapsed,
    resolveGuideWorkflow,
    setEditorGuideCollapsed,
} from '@/pages/automation/getting-started/guideWorkflowStorage';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {useAutomationGettingStartedQuery} from '@/shared/middleware/graphql';
import {
    ProjectDeploymentKeys,
    useGetWorkspaceProjectDeploymentsQuery,
} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {WorkflowExecutionKeys} from '@/shared/queries/automation/workflowExecutions.queries';
import {useGetWorkflowNodeOutputQuery} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';

import {EditorGuideItemI, collectTasks, getEditorGuideItems, isDeploymentLive} from './getEditorGuideItems';

const EDITOR_GUIDE_POLLING_INTERVAL = 3000;

interface UseEditorGettingStartedProps {
    projectId: number;
    projectWorkflowId: number;
}

interface UseEditorGettingStartedI {
    collapsed: boolean;
    completed: boolean;
    deployment?: ProjectDeployment;
    doneCount: number;
    firstRunExists: boolean;
    firstRunState: FirstRunStateType;
    items: EditorGuideItemI[];
    status?: AutomationGettingStartedStatusType;
    toggleCollapsed: () => void;
    visible: boolean;
}

export default function useEditorGettingStarted({
    projectId,
    projectWorkflowId,
}: UseEditorGettingStartedProps): UseEditorGettingStartedI {
    const [collapsed, setCollapsed] = useState(false);
    const [executionsViewed, setExecutionsViewed] = useState(false);
    const [incompleteAtLoad, setIncompleteAtLoad] = useState<boolean | undefined>();

    const completionInvalidatedRef = useRef(false);
    const progressSignatureRef = useRef<string | undefined>(undefined);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const taskList = useWorkflowDataStore((state) => state.workflow.tasks);
    const executionsSheetOpen = useProjectDeploymentWorkflowSheetStore(
        (state) => state.projectDeploymentWorkflowSheetOpen
    );
    const workflowId = useWorkflowDataStore((state) => state.workflow.id);

    const queryClient = useQueryClient();

    const canCreateProject = useHasWorkspaceScope(currentWorkspaceId, 'PROJECT_CREATE');

    const {data: projects} = useGetWorkspaceProjectsQuery({id: currentWorkspaceId}, canCreateProject);

    const guideWorkflow = canCreateProject ? resolveGuideWorkflow(getRememberedGuideWorkflow(), projects) : undefined;
    const isGuideWorkflow =
        guideWorkflow?.projectId === projectId && guideWorkflow.projectWorkflowId === projectWorkflowId;
    const queriesEnabled = canCreateProject && isGuideWorkflow;

    const {data: deployments} = useGetWorkspaceProjectDeploymentsQuery(
        {environmentId: currentEnvironmentId, id: currentWorkspaceId, projectId},
        queriesEnabled
    );

    const deployment = deployments?.[0];

    const httpClientTaskName = (taskList ?? []).find((task) => task.type.startsWith('httpClient/'))?.name;

    const {data: httpClientNodeOutput} = useGetWorkflowNodeOutputQuery(
        {environmentId: currentEnvironmentId, id: workflowId!, workflowNodeName: httpClientTaskName!},
        queriesEnabled && !!workflowId && !!httpClientTaskName
    );

    const httpClientTested = !!httpClientNodeOutput?.testOutputResponse;

    const guideProject = projects?.find((project) => project.id === projectId);

    const guideProjectLastPublishedDate = guideProject?.lastPublishedDate?.toISOString();
    const guideProjectPublished = !!(guideProject?.lastPublishedDate && guideProject.lastProjectVersion);
    const deploymentsSignature = JSON.stringify(
        (deployments ?? []).map((projectDeployment) => [
            projectDeployment.id,
            projectDeployment.enabled,
            projectDeployment.projectVersion,
            (projectDeployment.projectDeploymentWorkflows ?? []).map(
                (deploymentWorkflow) => deploymentWorkflow.enabled
            ),
        ])
    );

    const pollingEnabled = queriesEnabled && !collapsed && incompleteAtLoad === true;

    const {data: statusData, isFetchedAfterMount} = useAutomationGettingStartedQuery(
        {workspaceId: `${currentWorkspaceId}`},
        {
            enabled: queriesEnabled,
            refetchInterval: (query) =>
                pollingEnabled &&
                !query.state.error &&
                (!!query.state.data?.automationGettingStarted?.live || isDeploymentLive(deployment)) &&
                !hasFirstRun(query.state.data?.automationGettingStarted)
                    ? EDITOR_GUIDE_POLLING_INTERVAL
                    : false,
        }
    );

    const status = statusData?.automationGettingStarted;
    const statusLoaded = status !== undefined;
    const firstRunExists = hasFirstRun(status);
    const completed = !!status?.completed;

    const visible = queriesEnabled && statusLoaded && incompleteAtLoad === true;

    const firstRunState: FirstRunStateType = firstRunExists ? 'done' : status?.lastFailedRun ? 'failed' : 'waiting';

    const items = useMemo(
        () =>
            getEditorGuideItems({
                deployment,
                executionsViewed,
                guideProjectPublished,
                httpClientTested,
                status,
                tasks: collectTasks(taskList ?? []),
            }),
        [deployment, executionsViewed, guideProjectPublished, httpClientTested, status, taskList]
    );

    const doneCount = items.filter((item) => item.done).length;

    const toggleCollapsed = useCallback(() => {
        setEditorGuideCollapsed(!collapsed);

        setCollapsed(!collapsed);
    }, [collapsed]);

    useEffect(() => {
        if (statusLoaded && isFetchedAfterMount && incompleteAtLoad === undefined && queriesEnabled) {
            setIncompleteAtLoad(!firstRunExists);
        }
    }, [firstRunExists, incompleteAtLoad, isFetchedAfterMount, queriesEnabled, statusLoaded]);

    useEffect(() => {
        if (queriesEnabled && incompleteAtLoad === true && firstRunExists && !completionInvalidatedRef.current) {
            completionInvalidatedRef.current = true;

            void queryClient.invalidateQueries({queryKey: ['automationGettingStartedCompleted']});
            void queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});
            void queryClient.invalidateQueries({queryKey: WorkflowExecutionKeys.workflowExecutions});
        }
    }, [firstRunExists, incompleteAtLoad, queriesEnabled, queryClient]);

    useEffect(() => {
        setCollapsed(isEditorGuideCollapsed());
    }, []);

    useEffect(() => {
        if (queriesEnabled && firstRunExists && executionsSheetOpen) {
            setExecutionsViewed(true);
        }
    }, [executionsSheetOpen, firstRunExists, queriesEnabled]);

    useEffect(() => {
        if (!queriesEnabled || projects === undefined || deployments === undefined) {
            return;
        }

        const progressSignature = `${guideProjectLastPublishedDate}|${deploymentsSignature}`;

        if (progressSignatureRef.current !== undefined && progressSignatureRef.current !== progressSignature) {
            void queryClient.invalidateQueries({queryKey: ['automationGettingStarted']});
        }

        progressSignatureRef.current = progressSignature;
    }, [deployments, deploymentsSignature, guideProjectLastPublishedDate, projects, queriesEnabled, queryClient]);

    return {
        collapsed,
        completed,
        deployment,
        doneCount,
        firstRunExists,
        firstRunState,
        items,
        status,
        toggleCollapsed,
        visible,
    };
}
