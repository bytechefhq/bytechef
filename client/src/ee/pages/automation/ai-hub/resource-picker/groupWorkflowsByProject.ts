interface WorkflowProjectGroupI<T> {
    projectId: string;
    projectName: string;
    workflows: T[];
}

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
