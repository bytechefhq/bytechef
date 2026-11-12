import {AutomationProjectsQuery} from '@/shared/middleware/graphql';

type AutomationProjectWorkflowTemplateType =
    AutomationProjectsQuery['automationProjects'][number]['workflowTemplates'][number];

export const getPathAfterWorkflowDelete = (
    workflowTemplates: AutomationProjectWorkflowTemplateType[],
    deletedWorkflowUuid: string
): string => {
    const firstRemainingWorkflowTemplate = workflowTemplates.find(
        (workflowTemplate) => workflowTemplate.workflowUuid !== deletedWorkflowUuid
    );

    if (firstRemainingWorkflowTemplate) {
        return `/embedded/automations/${firstRemainingWorkflowTemplate.workflowUuid}/editor`;
    }

    return '/embedded/automations';
};
