import {Project, Workflow} from '@/shared/middleware/automation/configuration';

export const useProjectsLeftSidebar = () => {
    const getWorkflowsProjectId = (projects: Project[]) => {
        const workflowToProjectMap: Record<number, number> = {};

        projects.forEach((project) => {
            project.projectWorkflowIds?.forEach((workflowId) => {
                workflowToProjectMap[workflowId] = +(project.id || 0);
            });
        });

        return (workflow: Workflow) => workflowToProjectMap[workflow.projectWorkflowId || 0] || 0;
    };

    const getFilteredWorkflows = (workflows: Workflow[] | undefined, sortBy: string, searchValue: string) => {
        if (!workflows) {
            return [];
        }

        const sortFunctions = {
            alphabetical: (a: Workflow, b: Workflow) => (a.label || '').localeCompare(b.label || ''),
            'date-created': (a: Workflow, b: Workflow) =>
                (b.createdDate?.getTime() || 0) - (a.createdDate?.getTime() || 0),
            'last-edited': (a: Workflow, b: Workflow) =>
                (b.lastModifiedDate?.getTime() || 0) - (a.lastModifiedDate?.getTime() || 0),
            'reverse-alphabetical': (a: Workflow, b: Workflow) => (b.label || '').localeCompare(a.label || ''),
        };

        return [...workflows]
            .sort(sortFunctions[sortBy as keyof typeof sortFunctions] || (() => 0))
            .filter((workflow) => (workflow.label || '').toLowerCase().includes(searchValue.toLowerCase()));
    };

    const calculateTimeDifference = (date?: string) => {
        if (!date) {
            return 'Unknown';
        }

        const currentTimestamp = new Date();
        const workflowLastModifiedDate = new Date(date || '');
        const millisecondsDifference = currentTimestamp.getTime() - workflowLastModifiedDate.getTime();

        const seconds = Math.floor(millisecondsDifference / 1000);
        const minutes = Math.floor(seconds / 60);
        const hours = Math.floor(minutes / 60);
        const days = Math.floor(hours / 24);

        if (days > 7) {
            return `on ${workflowLastModifiedDate.toLocaleDateString()}`;
        }

        if (days > 0) {
            return `${days} day${days !== 1 ? 's' : ''} ago`;
        }

        if (hours > 0) {
            return `${hours} hour${hours !== 1 ? 's' : ''} ago`;
        }

        if (minutes > 0) {
            return `${minutes} minute${minutes !== 1 ? 's' : ''} ago`;
        }

        return `just now`;
    };

    return {
        calculateTimeDifference,
        getFilteredWorkflows,
        getWorkflowsProjectId,
    };
};
