import WorkflowsListItemDropdownMenu from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItemDropdownMenu';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import WorkflowTriggerAndComponentsRow from '@/shared/components/workflow/WorkflowTriggerAndComponentsRow';
import WorkflowsSidebarListItem from '@/shared/components/workflow/WorkflowsSidebarListItem';
import {Project, Workflow} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface WorkflowsListItemProps {
    calculateTimeDifference: (date: string) => string;
    workflow: Workflow;
    currentWorkflowId: string;
    onProjectClick: (projectId: number, projectWorkflowId: number) => void;
    findProjectIdByWorkflow: (workflow: Workflow) => number;
    project?: Project;
    setSelectedProjectId: (projectId: number) => void;
}

const WorkflowsListItem = ({
    calculateTimeDifference,
    currentWorkflowId,
    findProjectIdByWorkflow,
    onProjectClick,
    project,
    setSelectedProjectId,
    workflow,
}: WorkflowsListItemProps) => {
    const {componentDefinitions, taskDispatcherDefinitions} = useWorkflowDataStore(
        useShallow((state) => ({
            componentDefinitions: state.componentDefinitions,
            taskDispatcherDefinitions: state.taskDispatcherDefinitions,
        }))
    );

    const {filteredComponentNames, workflowComponentDefinitions, workflowTaskDispatcherDefinitions} = useMemo(() => {
        const componentNames = [
            ...(workflow.workflowTriggerComponentNames || []),
            ...(workflow.workflowTaskComponentNames || []),
        ];

        const uniqueComponentNames = Array.from(new Set(componentNames));

        const componentDefs: Record<string, ComponentDefinitionBasic | undefined> = {};
        const taskDispatcherDefs: Record<string, ComponentDefinitionBasic | undefined> = {};

        uniqueComponentNames.forEach((name) => {
            componentDefs[name] = componentDefinitions?.find((definition) => definition.name === name);

            taskDispatcherDefs[name] = taskDispatcherDefinitions?.find((definition) => definition.name === name);
        });

        return {
            filteredComponentNames: uniqueComponentNames,
            workflowComponentDefinitions: componentDefs,
            workflowTaskDispatcherDefinitions: taskDispatcherDefs,
        };
    }, [workflow, componentDefinitions, taskDispatcherDefinitions]);

    const projectId = findProjectIdByWorkflow(workflow);
    const projectWorkflowId = workflow.projectWorkflowId || 0;

    const timeAgo = useMemo(
        () => calculateTimeDifference(workflow?.lastModifiedDate?.toString() || ''),
        [calculateTimeDifference, workflow?.lastModifiedDate]
    );

    const handleCardClick = () => {
        onProjectClick(projectId, projectWorkflowId);

        setSelectedProjectId(projectId);
    };

    return (
        <WorkflowsSidebarListItem
            active={workflow.id === currentWorkflowId}
            components={
                <WorkflowTriggerAndComponentsRow
                    filteredComponentNames={filteredComponentNames}
                    fitToWidth
                    workflow={workflow}
                    workflowComponentDefinitions={workflowComponentDefinitions}
                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                />
            }
            editedTimeAgo={timeAgo}
            label={workflow.label}
            menu={
                project && (
                    <WorkflowsListItemDropdownMenu
                        currentWorkflowId={currentWorkflowId}
                        project={project}
                        workflow={workflow}
                    />
                )
            }
            onClick={handleCardClick}
        />
    );
};

export default WorkflowsListItem;
