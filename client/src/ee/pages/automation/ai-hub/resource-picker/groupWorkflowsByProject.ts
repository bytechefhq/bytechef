interface WorkflowProjectGroupI<T> {
    projectId: string;
    projectName: string;
    workflows: T[];
}

/**
 * Group workflow items by their parent project, preserving the order in which each project is first
 * seen (so the project list stays stable across re-renders). Generic over any item carrying
 * `projectId` + `projectName`, so both pickers' `WorkflowItemI` shapes work without coupling.
 */
export function groupWorkflowsByProject<T extends {projectId: string; projectName: string}>(
    workflows: T[]
): WorkflowProjectGroupI<T>[] {
    const order: string[] = [];
    const byProject = new Map<string, WorkflowProjectGroupI<T>>();

    for (const workflow of workflows) {
        const existing = byProject.get(workflow.projectId);

        if (existing) {
            existing.workflows.push(workflow);
        } else {
            order.push(workflow.projectId);

            byProject.set(workflow.projectId, {
                projectId: workflow.projectId,
                projectName: workflow.projectName,
                workflows: [workflow],
            });
        }
    }

    return order.map((projectId) => byProject.get(projectId)!);
}
