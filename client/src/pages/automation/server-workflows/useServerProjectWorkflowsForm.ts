import getWorkflowComponentConnections, {
    splitSubflowDeploymentValues,
} from '@/pages/automation/project-deployments/components/project-deployment-dialog/projectDeploymentDialog-utils';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    ProjectDeployment,
    ProjectDeploymentWorkflow,
    ProjectDeploymentWorkflowConnection,
} from '@/shared/middleware/automation/configuration';
import {WorkflowConfigurationInput} from '@/shared/middleware/graphql';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {useGetProjectDeploymentQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {useCallback, useEffect, useMemo, useRef} from 'react';
import {useForm} from 'react-hook-form';

interface UseServerProjectWorkflowsFormProps {
    eligibleWorkflowIds?: string[];
    environmentId?: number;
    projectDeploymentId?: number;
    projectId?: number;
    projectVersion?: number;
}

export default function useServerProjectWorkflowsForm({
    eligibleWorkflowIds,
    environmentId,
    projectDeploymentId,
    projectId,
    projectVersion,
}: UseServerProjectWorkflowsFormProps) {
    const initializedKeyRef = useRef('');

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const form = useForm<ProjectDeployment>({defaultValues: {projectDeploymentWorkflows: []}});

    const {data: projectVersionWorkflows} = useGetProjectVersionWorkflowsQuery(
        projectId!,
        projectVersion!,
        true,
        !!projectId && !!projectVersion,
        false
    );

    const {data: projectDeployment} = useGetProjectDeploymentQuery(projectDeploymentId!, !!projectDeploymentId);

    const connectionsEnvironmentId = environmentId ?? projectDeployment?.environmentId;

    const {data: connections} = useGetWorkspaceConnectionsQuery(
        {environmentId: connectionsEnvironmentId, id: currentWorkspaceId!},
        !!currentWorkspaceId && connectionsEnvironmentId !== undefined
    );

    const isProjectDeploymentPending = !!projectDeploymentId && !projectDeployment;

    const workflows = useMemo(() => {
        if (!projectVersionWorkflows || !eligibleWorkflowIds || isProjectDeploymentPending) {
            return undefined;
        }

        return projectVersionWorkflows.filter((workflow) => eligibleWorkflowIds.includes(workflow.id!));
    }, [eligibleWorkflowIds, isProjectDeploymentPending, projectVersionWorkflows]);

    const getWorkflowConfigurations = useCallback(
        (selectedWorkflowIds: string[]): WorkflowConfigurationInput[] => {
            const formWorkflows = form.getValues('projectDeploymentWorkflows') ?? [];

            return selectedWorkflowIds.flatMap((workflowId) => {
                const workflow = workflows?.find((curWorkflow) => curWorkflow.id === workflowId);

                if (!workflow) {
                    return [];
                }

                const formWorkflow = formWorkflows.find((curFormWorkflow) => curFormWorkflow.workflowId === workflowId);

                const {ownValues} = splitSubflowDeploymentValues(
                    workflow,
                    [workflow],
                    formWorkflow?.connections,
                    formWorkflow?.inputs as Record<string, unknown> | undefined
                );

                return [
                    {
                        connections: ownValues.connections.map((connection) => ({
                            connectionId: String(connection.connectionId),
                            workflowConnectionKey: connection.workflowConnectionKey,
                            workflowNodeName: connection.workflowNodeName,
                        })),
                        inputs: ownValues.inputs,
                        workflowId,
                    },
                ];
            });
        },
        [form, workflows]
    );

    useEffect(() => {
        if (!workflows) {
            return;
        }

        const initializationKey = `${projectId}-${projectVersion}-${projectDeployment?.id ?? ''}-${workflows
            .map((workflow) => workflow.id)
            .join(',')}`;

        if (initializedKeyRef.current === initializationKey) {
            return;
        }

        initializedKeyRef.current = initializationKey;

        const projectDeploymentWorkflows: ProjectDeploymentWorkflow[] = workflows.map((workflow) => {
            const existingProjectDeploymentWorkflow = projectDeployment?.projectDeploymentWorkflows?.find(
                (projectDeploymentWorkflow) => projectDeploymentWorkflow.workflowId === workflow.id
            );

            const workflowConnections = getWorkflowComponentConnections(workflow, [workflow]).map(
                (componentConnection) =>
                    existingProjectDeploymentWorkflow?.connections?.find(
                        (connection) =>
                            connection.workflowNodeName === componentConnection.workflowNodeName &&
                            connection.workflowConnectionKey === componentConnection.key
                    ) ??
                    ({
                        workflowConnectionKey: componentConnection.key,
                        workflowNodeName: componentConnection.workflowNodeName,
                    } as ProjectDeploymentWorkflowConnection)
            );

            return {
                connections: workflowConnections,
                inputs: existingProjectDeploymentWorkflow?.inputs ?? {},
                workflowId: workflow.id!,
            };
        });

        form.reset({projectDeploymentWorkflows});
    }, [form, projectDeployment, projectId, projectVersion, workflows]);

    return {connections, form, getWorkflowConfigurations, workflows};
}
