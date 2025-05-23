import {Button} from '@/components/ui/button';
import {ScrollArea} from '@/components/ui/scroll-area';
import ProjectSelect from '@/pages/automation/project/components/projects-sidebar/components/ProjectSelect';
import ProjectWorkflowsList from '@/pages/automation/project/components/projects-sidebar/components/ProjectWorkflowsList';
import WorkflowsListFilter from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListFilter';
import WorkflowsListItem from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItem';
import WorkflowsListSkeleton from '@/pages/automation/project/components/projects-sidebar/components/WorkflowsListSkeleton';
import {useProjectsLeftSidebar} from '@/pages/automation/project/components/projects-sidebar/hooks/useProjectsLeftSidebar';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {useGetProjectWorkflowsQuery, useGetWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {PlusIcon} from 'lucide-react';
import {RefObject, useEffect, useMemo, useState} from 'react';
import {ImperativePanelHandle} from 'react-resizable-panels';

interface ProjectsLeftSidebarProps {
    bottomResizablePanelRef: RefObject<ImperativePanelHandle>;
    onProjectClick: (projectId: number, projectWorkflowId: number) => void;
    projectId: number;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    currentWorkflowId: string;
}

const ProjectsLeftSidebar = ({
    bottomResizablePanelRef,
    currentWorkflowId,
    onProjectClick,
    projectId,
    updateWorkflowMutation,
}: ProjectsLeftSidebarProps) => {
    const [selectedProjectId, setSelectedProjectId] = useState(projectId || 0);
    const [sortBy, setSortBy] = useState('last-edited');
    const [searchValue, setSearchValue] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const {data: eachProjectWorkflows, isLoading: projectWorkflowsLoading} = useGetProjectWorkflowsQuery(
        selectedProjectId,
        selectedProjectId !== 0
    );
    const {data: allProjectsWorkflows, isLoading: allProjectsWorkflowsLoading} = useGetWorkflowsQuery(
        selectedProjectId === 0
    );
    const workflows = eachProjectWorkflows || allProjectsWorkflows;

    const {calculateTimeDifference, createProjectWorkflowMutation, getFilteredWorkflows, getWorkflowsProjectId} =
        useProjectsLeftSidebar({
            bottomResizablePanelRef,
            projectId,
        });

    const {currentWorkspaceId} = useWorkspaceStore();

    const {data: projects, refetch: refetchProjects} = useGetWorkspaceProjectsQuery({
        id: currentWorkspaceId!,
    });

    const findProjectIdByWorkflow = getWorkflowsProjectId(projects || []);

    const filteredWorkflowsList = useMemo(
        () => getFilteredWorkflows(workflows, sortBy, searchValue),
        [workflows, sortBy, searchValue, getFilteredWorkflows]
    );

    useEffect(() => {
        setIsLoading(projectWorkflowsLoading || allProjectsWorkflowsLoading);
    }, [projectWorkflowsLoading, allProjectsWorkflowsLoading]);

    useEffect(() => {
        if (selectedProjectId === 0) {
            refetchProjects();
        }
    }, [selectedProjectId, refetchProjects]);

    return (
        <aside className="flex h-full flex-col items-center gap-2 bg-surface-main pt-0.5">
            <div className="flex flex-col gap-2 px-4">
                {projects && (
                    <ProjectSelect
                        projectId={projectId}
                        projects={projects}
                        selectedProjectId={selectedProjectId}
                        setSelectedProjectId={setSelectedProjectId}
                    />
                )}

                <WorkflowsListFilter
                    searchValue={searchValue}
                    setSearchValue={setSearchValue}
                    setSortBy={setSortBy}
                    sortBy={sortBy}
                />

                {selectedProjectId === projectId && (
                    <WorkflowDialog
                        createWorkflowMutation={createProjectWorkflowMutation}
                        projectId={projectId}
                        triggerNode={
                            <Button
                                className="w-full bg-surface-neutral-secondary py-2 hover:bg-background [&_svg]:size-5"
                                size="icon"
                                variant="ghost"
                            >
                                <div className="flex items-center justify-center gap-2">
                                    <PlusIcon />

                                    <span>New workflow</span>
                                </div>
                            </Button>
                        }
                        updateWorkflowMutation={updateWorkflowMutation}
                        useGetWorkflowQuery={useGetWorkflowQuery}
                    />
                )}
            </div>

            <ScrollArea className="mb-3 h-screen w-full overflow-y-auto px-4">
                {isLoading && <WorkflowsListSkeleton />}

                {!isLoading && (
                    <ul className="flex flex-col items-center gap-2">
                        {selectedProjectId === 0 &&
                            (projects || []).map((project) => (
                                <ProjectWorkflowsList
                                    calculateTimeDifference={calculateTimeDifference}
                                    currentWorkflowId={currentWorkflowId}
                                    filteredWorkflowsList={filteredWorkflowsList}
                                    findProjectIdByWorkflow={findProjectIdByWorkflow}
                                    key={project.id}
                                    onProjectClick={onProjectClick}
                                    project={project}
                                    setSelectedProjectId={setSelectedProjectId}
                                />
                            ))}

                        {selectedProjectId !== 0 &&
                            filteredWorkflowsList.map((workflow) => (
                                <WorkflowsListItem
                                    calculateTimeDifference={calculateTimeDifference}
                                    currentWorkflowId={currentWorkflowId}
                                    findProjectIdByWorkflow={findProjectIdByWorkflow}
                                    key={workflow.id}
                                    onProjectClick={onProjectClick}
                                    setSelectedProjectId={setSelectedProjectId}
                                    workflow={workflow}
                                />
                            ))}
                    </ul>
                )}
            </ScrollArea>
        </aside>
    );
};

export default ProjectsLeftSidebar;
