interface WorkflowExecutionsFiltersParamsI {
    endDate?: Date;
    projectDeploymentId?: number;
    projectDeployments?: Array<{id?: number; name?: string; projectVersion?: number}>;
    projectId?: number;
    projects?: Array<{id?: number; name?: string}>;
    startDate?: Date;
    status?: string;
    workflowId?: string;
    workflows?: Array<{id?: string; label?: string}>;
}

export const getWorkflowExecutionsFilters = ({
    endDate,
    projectDeploymentId,
    projectDeployments,
    projectId,
    projects,
    startDate,
    status,
    workflowId,
    workflows,
}: WorkflowExecutionsFiltersParamsI): Array<{label: string; value?: string}> => {
    let projectLabel: string | undefined;

    if (projectId) {
        projectLabel = projects?.find((project) => project.id === projectId)?.name || String(projectId);
    }

    let projectDeploymentLabel: string | undefined;

    if (projectDeploymentId) {
        const projectDeployment = projectDeployments?.find(
            (projectDeployment) => projectDeployment.id === projectDeploymentId
        );

        projectDeploymentLabel = projectDeployment
            ? `${projectDeployment.name} V${projectDeployment.projectVersion}`
            : String(projectDeploymentId);
    }

    let workflowLabel: string | undefined;

    if (workflowId) {
        workflowLabel = workflows?.find((workflow) => workflow.id === workflowId)?.label || workflowId;
    }

    return [
        {label: 'Status', value: status},
        {label: 'Start date', value: startDate?.toLocaleDateString()},
        {label: 'End date', value: endDate?.toLocaleDateString()},
        {label: 'Project', value: projectLabel},
        {label: 'Deployment', value: projectDeploymentLabel},
        {label: 'Workflow', value: workflowLabel},
    ];
};
