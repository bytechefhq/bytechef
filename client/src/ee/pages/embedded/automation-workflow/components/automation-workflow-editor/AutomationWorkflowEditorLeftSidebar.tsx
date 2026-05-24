import Button from '@/components/Button/Button';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Skeleton} from '@/components/ui/skeleton';
import AutomationWorkflowEditorProjectSelect from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorProjectSelect';
import AutomationWorkflowEditorWorkflowsFilter from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowsFilter';
import AutomationWorkflowEditorWorkflowsListItem from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowsListItem';
import AutomationWorkflowDialog, {
    AutomationWorkflowFormValuesI,
} from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-dialog/AutomationWorkflowDialog';
import AutomationWorkflowProjectDialog, {
    AutomationWorkflowProjectFormValuesI,
} from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-dialog/AutomationWorkflowProjectDialog';
import {
    AutomationWorkflowProjectsQuery,
    useAutomationWorkflowProjectCategoriesQuery,
    useAutomationWorkflowProjectTagsQuery,
    useAutomationWorkflowProjectsQuery,
    useCreateAutomationWorkflowProjectMutation,
    useCreateAutomationWorkflowProjectWorkflowMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {useMemo, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectWorkflowTemplateType = AutomationWorkflowProjectType['workflowTemplates'][number];

interface AutomationWorkflowEditorLeftSidebarProps {
    currentWorkflowId: string;
}

const AutomationWorkflowEditorLeftSidebar = ({currentWorkflowId}: AutomationWorkflowEditorLeftSidebarProps) => {
    const [searchValue, setSearchValue] = useState('');
    const [showProjectDialog, setShowProjectDialog] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);
    const [sortBy, setSortBy] = useState('last-edited');

    const searchInputRef = useRef<HTMLInputElement>(null);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {data: projectsData, isLoading: projectsIsLoading} = useAutomationWorkflowProjectsQuery();
    const {data: categoriesData} = useAutomationWorkflowProjectCategoriesQuery();
    const {data: tagsData} = useAutomationWorkflowProjectTagsQuery();

    const categories = categoriesData?.automationWorkflowProjectCategories;
    const tags = tagsData?.automationWorkflowProjectTags;

    const projects = projectsData?.automationWorkflowProjects ?? [];

    const currentProject = projects.find((automationWorkflowProject) =>
        automationWorkflowProject.workflowTemplates.some(
            (projectWorkflow) => projectWorkflow.workflowUuid === currentWorkflowId
        )
    );

    const [selectedProjectId, setSelectedProjectId] = useState<string>(currentProject?.id ?? '');

    const selectedProject: AutomationWorkflowProjectType | undefined = projects.find(
        (automationWorkflowProject) => automationWorkflowProject.id === selectedProjectId
    );

    const createProjectMutation = useCreateAutomationWorkflowProjectMutation();
    const createWorkflowMutation = useCreateAutomationWorkflowProjectWorkflowMutation();

    const filteredAndSortedWorkflows = useMemo<AutomationWorkflowProjectWorkflowTemplateType[]>(() => {
        const sourceWorkflows = selectedProject?.workflowTemplates ?? [];

        const filtered = sourceWorkflows.filter((automationWorkflowProjectWorkflow) => {
            if (!searchValue) {
                return true;
            }

            const workflowLabel =
                automationWorkflowProjectWorkflow.label ?? automationWorkflowProjectWorkflow.workflowUuid;

            return workflowLabel.toLowerCase().includes(searchValue.toLowerCase());
        });

        if (sortBy === 'last-edited') {
            return [...filtered].sort((firstWorkflow, secondWorkflow) => {
                const firstDate = firstWorkflow.lastModifiedDate ?? '';
                const secondDate = secondWorkflow.lastModifiedDate ?? '';

                return secondDate.localeCompare(firstDate);
            });
        }

        return [...filtered].sort((firstWorkflow, secondWorkflow) => {
            const firstLabel = firstWorkflow.label ?? firstWorkflow.workflowUuid;
            const secondLabel = secondWorkflow.label ?? secondWorkflow.workflowUuid;

            return firstLabel.localeCompare(secondLabel);
        });
    }, [selectedProject, searchValue, sortBy]);

    const handleWorkflowClick = (workflowUuid: string) => {
        if (workflowUuid !== currentWorkflowId) {
            navigate(`/embedded/automation-workflows/${workflowUuid}/editor`);
        }
    };

    const handleProjectDialogSubmit = (values: AutomationWorkflowProjectFormValuesI) => {
        createProjectMutation.mutate(
            {
                category: values.category,
                description: values.description,
                name: values.name,
                tags: values.tags,
            },
            {
                onSuccess: () => {
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});
                },
            }
        );

        setShowProjectDialog(false);
    };

    const handleWorkflowDialogSubmit = (values: AutomationWorkflowFormValuesI) => {
        const targetProject = selectedProject || currentProject;

        if (!targetProject) {
            return;
        }

        const definition = JSON.stringify({
            description: values.description,
            inputs: [],
            label: values.label,
            tasks: [],
            triggers: [],
        });

        createWorkflowMutation.mutate(
            {definition, projectId: targetProject.id},
            {
                onSuccess: (data) => {
                    queryClient.invalidateQueries({queryKey: ['automationWorkflowProjects']});

                    navigate(`/embedded/automation-workflows/${data.createAutomationWorkflowProjectWorkflow}/editor`);
                },
            }
        );

        setShowWorkflowDialog(false);
    };

    return (
        <aside className="flex h-full min-w-[355px] flex-col items-center gap-2 bg-surface-main px-4 pt-3">
            <div className="flex w-full flex-col gap-2">
                <div className="flex items-center gap-2">
                    <AutomationWorkflowEditorProjectSelect
                        projectId={currentProject?.id ?? ''}
                        projects={projects}
                        selectedProjectId={selectedProjectId}
                        setSelectedProjectId={setSelectedProjectId}
                    />

                    <Button
                        aria-label="New project"
                        icon={<PlusIcon />}
                        onClick={() => setShowProjectDialog(true)}
                        size="icon"
                        variant="outline"
                    />
                </div>

                <AutomationWorkflowEditorWorkflowsFilter
                    ref={searchInputRef}
                    searchValue={searchValue}
                    setSearchValue={setSearchValue}
                    setSortBy={setSortBy}
                    sortBy={sortBy}
                />

                <Button
                    className="w-full [&_svg]:size-5"
                    icon={<PlusIcon />}
                    label="Workflow"
                    onClick={() => setShowWorkflowDialog(true)}
                    variant="secondary"
                />
            </div>

            <ScrollArea className="mb-3 h-screen w-full overflow-y-auto">
                {projectsIsLoading && (
                    <div className="flex flex-col gap-2">
                        <Skeleton className="h-9 w-full rounded-md" />

                        <Skeleton className="h-9 w-full rounded-md" />

                        <Skeleton className="h-9 w-full rounded-md" />
                    </div>
                )}

                {!projectsIsLoading && filteredAndSortedWorkflows.length > 0 && (
                    <ul className="flex flex-col items-center gap-4">
                        {filteredAndSortedWorkflows.map((workflow) => (
                            <AutomationWorkflowEditorWorkflowsListItem
                                currentWorkflowId={currentWorkflowId}
                                key={workflow.workflowUuid}
                                onWorkflowClick={handleWorkflowClick}
                                workflow={workflow}
                            />
                        ))}
                    </ul>
                )}

                {!projectsIsLoading && filteredAndSortedWorkflows.length === 0 && (
                    <span className="text-sm text-muted-foreground">No workflows found</span>
                )}
            </ScrollArea>

            {showProjectDialog && (
                <AutomationWorkflowProjectDialog
                    categories={categories}
                    onClose={() => setShowProjectDialog(false)}
                    onSubmit={handleProjectDialogSubmit}
                    tags={tags}
                />
            )}

            {showWorkflowDialog && (
                <AutomationWorkflowDialog
                    onClose={() => setShowWorkflowDialog(false)}
                    onSubmit={handleWorkflowDialogSubmit}
                />
            )}
        </aside>
    );
};

export default AutomationWorkflowEditorLeftSidebar;
