import AutomationWorkflowEditorWorkflowsListItemDropdownMenu from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowsListItemDropdownMenu';
import {WorkflowComponentIconDefinitionType} from '@/pages/automation/project/components/projects-sidebar/components/WorkflowComponentsIcon';
import {useProjectsLeftSidebar} from '@/pages/automation/project/components/projects-sidebar/hooks/useProjectsLeftSidebar';
import WorkflowComponentsList from '@/shared/components/WorkflowComponentsList';
import WorkflowsSidebarListItem from '@/shared/components/workflow/WorkflowsSidebarListItem';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectWorkflowTemplateType = AutomationWorkflowProjectType['workflowTemplates'][number];

interface AutomationWorkflowEditorWorkflowsListItemProps {
    currentWorkflowId: string;
    onWorkflowClick: (workflowUuid: string) => void;
    project: AutomationWorkflowProjectType;
    workflow: AutomationWorkflowProjectWorkflowTemplateType;
}

const AutomationWorkflowEditorWorkflowsListItem = ({
    currentWorkflowId,
    onWorkflowClick,
    project,
    workflow,
}: AutomationWorkflowEditorWorkflowsListItemProps) => {
    const {calculateTimeDifference} = useProjectsLeftSidebar();

    const editedTimeAgo = workflow.lastModifiedDate ? calculateTimeDifference(workflow.lastModifiedDate) : undefined;

    const {filteredComponentNames, workflowComponentDefinitions} = useMemo(() => {
        const componentNames: string[] = [];
        const componentDefinitions: Record<string, WorkflowComponentIconDefinitionType | undefined> = {};

        [...workflow.triggers, ...workflow.components].forEach((workflowComponent) => {
            if (componentDefinitions[workflowComponent.name]) {
                return;
            }

            componentNames.push(workflowComponent.name);

            componentDefinitions[workflowComponent.name] = {
                icon: workflowComponent.icon ?? undefined,
                name: workflowComponent.name,
                title: workflowComponent.title ?? undefined,
            };
        });

        return {filteredComponentNames: componentNames, workflowComponentDefinitions: componentDefinitions};
    }, [workflow.components, workflow.triggers]);

    return (
        <WorkflowsSidebarListItem
            active={workflow.workflowUuid === currentWorkflowId}
            components={
                <WorkflowComponentsList
                    filteredComponentNames={filteredComponentNames}
                    fitToWidth
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={{}}
                />
            }
            editedTimeAgo={editedTimeAgo}
            label={workflow.label}
            menu={
                <AutomationWorkflowEditorWorkflowsListItemDropdownMenu
                    currentWorkflowId={currentWorkflowId}
                    project={project}
                    workflow={workflow}
                />
            }
            onClick={() => onWorkflowClick(workflow.workflowUuid)}
        />
    );
};

export default AutomationWorkflowEditorWorkflowsListItem;
