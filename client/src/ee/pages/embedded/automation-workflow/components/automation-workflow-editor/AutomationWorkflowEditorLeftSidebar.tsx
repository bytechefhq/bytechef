import {ScrollArea} from '@/components/ui/scroll-area';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AutomationWorkflowEditorProjectSelect, {
    ALL_PROJECTS_VALUE,
} from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorProjectSelect';
import AutomationWorkflowEditorWorkflowsFilter from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowsFilter';
import AutomationWorkflowEditorWorkflowsListItem from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowsListItem';
import {AutomationWorkflowProjectsQuery, useAutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {useEffect, useMemo, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectWorkflowTemplateType = AutomationWorkflowProjectType['workflowTemplates'][number];

interface ProjectWorkflowsGroupI {
    project: AutomationWorkflowProjectType;
    workflows: AutomationWorkflowProjectWorkflowTemplateType[];
}

const getWorkflowLabel = (workflow: AutomationWorkflowProjectWorkflowTemplateType) =>
    workflow.label ?? workflow.workflowUuid;

const filterAndSortWorkflows = (
    workflows: AutomationWorkflowProjectWorkflowTemplateType[],
    searchValue: string,
    sortBy: string
) => {
    const filteredWorkflows = workflows.filter((workflow) =>
        getWorkflowLabel(workflow).toLowerCase().includes(searchValue.toLowerCase())
    );

    if (sortBy === 'last-edited') {
        return filteredWorkflows.sort((firstWorkflow, secondWorkflow) =>
            (secondWorkflow.lastModifiedDate ?? '').localeCompare(firstWorkflow.lastModifiedDate ?? '')
        );
    }

    if (sortBy === 'reverse-alphabetical') {
        return filteredWorkflows.sort((firstWorkflow, secondWorkflow) =>
            getWorkflowLabel(secondWorkflow).localeCompare(getWorkflowLabel(firstWorkflow))
        );
    }

    return filteredWorkflows.sort((firstWorkflow, secondWorkflow) =>
        getWorkflowLabel(firstWorkflow).localeCompare(getWorkflowLabel(secondWorkflow))
    );
};

interface AutomationWorkflowEditorLeftSidebarProps {
    currentWorkflowId: string;
}

const AutomationWorkflowEditorLeftSidebar = ({currentWorkflowId}: AutomationWorkflowEditorLeftSidebarProps) => {
    const [searchValue, setSearchValue] = useState('');
    const [selectedProjectId, setSelectedProjectId] = useState<string>('');
    const [sortBy, setSortBy] = useState('last-edited');

    const searchInputRef = useRef<HTMLInputElement>(null);

    const navigate = useNavigate();

    const {data: projectsData, isLoading: projectsIsLoading} = useAutomationWorkflowProjectsQuery();

    const projects = useMemo(() => projectsData?.automationWorkflowProjects ?? [], [projectsData]);

    const currentProject = projects.find((automationWorkflowProject) =>
        automationWorkflowProject.workflowTemplates.some(
            (projectWorkflow) => projectWorkflow.workflowUuid === currentWorkflowId
        )
    );

    const allProjectsSelected = selectedProjectId === ALL_PROJECTS_VALUE;

    const projectWorkflowsGroups = useMemo<ProjectWorkflowsGroupI[]>(() => {
        const visibleProjects = allProjectsSelected
            ? projects
            : projects.filter((automationWorkflowProject) => automationWorkflowProject.id === selectedProjectId);

        return visibleProjects
            .map((automationWorkflowProject) => ({
                project: automationWorkflowProject,
                workflows: filterAndSortWorkflows(automationWorkflowProject.workflowTemplates, searchValue, sortBy),
            }))
            .filter((projectWorkflowsGroup) => projectWorkflowsGroup.workflows.length > 0);
    }, [allProjectsSelected, projects, searchValue, selectedProjectId, sortBy]);

    const handleWorkflowClick = (workflowUuid: string) => {
        if (workflowUuid !== currentWorkflowId) {
            navigate(`/embedded/automation-workflows/${workflowUuid}/editor`);
        }
    };

    const renderWorkflowsListItems = ({project, workflows}: ProjectWorkflowsGroupI) =>
        workflows.map((workflow) => (
            <AutomationWorkflowEditorWorkflowsListItem
                currentWorkflowId={currentWorkflowId}
                key={workflow.workflowUuid}
                onWorkflowClick={handleWorkflowClick}
                project={project}
                workflow={workflow}
            />
        ));

    useEffect(() => {
        if (!currentProject) {
            return;
        }

        setSelectedProjectId((previousSelectedProjectId) => previousSelectedProjectId || currentProject.id);
    }, [currentProject]);

    return (
        <aside className="flex h-full min-w-[355px] flex-col items-center gap-2 bg-surface-main px-4 pt-3">
            <div className="flex w-full flex-col gap-2">
                {projectsIsLoading ? (
                    <Skeleton className="h-9 w-full rounded-md" />
                ) : (
                    <div className="flex items-center gap-2">
                        <AutomationWorkflowEditorProjectSelect
                            projectId={currentProject?.id ?? ''}
                            projects={projects}
                            selectedProjectId={selectedProjectId}
                            setSelectedProjectId={setSelectedProjectId}
                        />
                    </div>
                )}

                <AutomationWorkflowEditorWorkflowsFilter
                    ref={searchInputRef}
                    searchValue={searchValue}
                    setSearchValue={setSearchValue}
                    setSortBy={setSortBy}
                    sortBy={sortBy}
                />
            </div>

            <ScrollArea className="mb-3 min-h-0 w-full flex-1 [&_[data-radix-scroll-area-viewport]>div]:block!">
                {projectsIsLoading && (
                    <div className="flex flex-col gap-2">
                        <Skeleton className="h-9 w-full rounded-md" />

                        <Skeleton className="h-9 w-full rounded-md" />

                        <Skeleton className="h-9 w-full rounded-md" />
                    </div>
                )}

                {!projectsIsLoading && projectWorkflowsGroups.length > 0 && (
                    <ul className="flex flex-col gap-4">
                        {allProjectsSelected
                            ? projectWorkflowsGroups.map((projectWorkflowsGroup) => (
                                  <li
                                      className="max-w-full border-b border-stroke-neutral-secondary pb-4 last:border-b-0 last:pb-0"
                                      key={projectWorkflowsGroup.project.id}
                                  >
                                      <Tooltip>
                                          <TooltipTrigger asChild>
                                              <h2 className="truncate rounded-md px-1 py-2 text-lg font-medium">
                                                  {projectWorkflowsGroup.project.name}
                                              </h2>
                                          </TooltipTrigger>

                                          {projectWorkflowsGroup.project.name.length > 25 && (
                                              <TooltipContent className="max-w-80">
                                                  {projectWorkflowsGroup.project.name}
                                              </TooltipContent>
                                          )}
                                      </Tooltip>

                                      <ul className="flex flex-col gap-2">
                                          {renderWorkflowsListItems(projectWorkflowsGroup)}
                                      </ul>
                                  </li>
                              ))
                            : renderWorkflowsListItems(projectWorkflowsGroups[0])}
                    </ul>
                )}

                {!projectsIsLoading && projectWorkflowsGroups.length === 0 && (
                    <span className="block w-full py-2 text-sm text-muted-foreground">No workflows found</span>
                )}
            </ScrollArea>
        </aside>
    );
};

export default AutomationWorkflowEditorLeftSidebar;
