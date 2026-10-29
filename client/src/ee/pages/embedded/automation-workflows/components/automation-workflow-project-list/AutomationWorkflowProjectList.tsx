import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {ButtonGroup} from '@/components/ui/button-group';
import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectTagsQuery,
    AutomationWorkflowProjectsQuery,
} from '@/shared/middleware/graphql';
import {ChevronDownIcon, PlusIcon, UploadIcon} from 'lucide-react';
import {useEffect, useState} from 'react';

import AutomationWorkflowProjectConfigurationDialog from '../automation-workflow-project-configuration-dialog/AutomationWorkflowProjectConfigurationDialog';
import AutomationWorkflowProjectConfigurationList from '../automation-workflow-project-configuration-list/AutomationWorkflowProjectConfigurationList';
import AutomationWorkflowProjectListItem from './AutomationWorkflowProjectListItem';
import AutomationWorkflowProjectListItemTabs, {
    AutomationWorkflowProjectListItemTabType,
} from './AutomationWorkflowProjectListItemTabs';
import AutomationWorkflowProjectWorkflowList from './AutomationWorkflowProjectWorkflowList';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];
type EmbeddedTagType = AutomationWorkflowProjectTagsQuery['automationWorkflowProjectTags'][number];

interface AutomationWorkflowProjectListProps {
    newlyCreatedProjectId?: string;
    onCreateWorkflow: (projectId: string) => void;
    onDeleteProject: (projectId: string) => void;
    onDeleteWorkflow: (workflowUuid: string) => void;
    onEditProject: (project: AutomationWorkflowProjectType) => void;
    onEditWorkflow: (workflow: AutomationWorkflowProjectType['workflowTemplates'][number]) => void;
    onImportWorkflow: (projectId: string) => void;
    onPublishProject: (projectId: string) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    onUpdateTags: (project: AutomationWorkflowProjectType, tagNames: string[]) => void;
    projects: Array<AutomationWorkflowProjectType>;
    tags: EmbeddedTagType[];
}

const AutomationWorkflowProjectList = ({
    newlyCreatedProjectId,
    onCreateWorkflow,
    onDeleteProject,
    onDeleteWorkflow,
    onEditProject,
    onEditWorkflow,
    onImportWorkflow,
    onPublishProject,
    onSelectWorkflow,
    onUpdateTags,
    projects,
    tags,
}: AutomationWorkflowProjectListProps) => {
    const [activeTabs, setActiveTabs] = useState<Record<string, AutomationWorkflowProjectListItemTabType>>({});
    const [configurationDialog, setConfigurationDialog] = useState<
        {configuration?: AutomationWorkflowProjectConfigurationType; project: AutomationWorkflowProjectType} | undefined
    >();
    const [openProjectIds, setOpenProjectIds] = useState<Set<string>>(new Set());

    useEffect(() => {
        if (newlyCreatedProjectId) {
            setOpenProjectIds((previousOpenProjectIds) => {
                const nextOpenProjectIds = new Set(previousOpenProjectIds);

                nextOpenProjectIds.add(newlyCreatedProjectId);

                return nextOpenProjectIds;
            });
        }
    }, [newlyCreatedProjectId]);

    return (
        <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
            {projects.map((project) => (
                <Collapsible
                    className="group mb-2 rounded border border-border/50"
                    key={project.id}
                    onOpenChange={(open) => {
                        setOpenProjectIds((previousOpenProjectIds) => {
                            const nextOpenProjectIds = new Set(previousOpenProjectIds);

                            if (open) {
                                nextOpenProjectIds.add(project.id);
                            } else {
                                nextOpenProjectIds.delete(project.id);
                            }

                            return nextOpenProjectIds;
                        });
                    }}
                    open={openProjectIds.has(project.id)}
                >
                    <AutomationWorkflowProjectListItem
                        onCreateWorkflow={onCreateWorkflow}
                        onDeleteProject={onDeleteProject}
                        onEditProject={onEditProject}
                        onImportWorkflow={onImportWorkflow}
                        onPublishProject={onPublishProject}
                        onSelectWorkflow={onSelectWorkflow}
                        onUpdateTags={onUpdateTags}
                        project={project}
                        tags={tags}
                    />

                    <CollapsibleContent>
                        <AutomationWorkflowProjectListItemTabs
                            actions={
                                (activeTabs[project.id] || 'workflows') === 'workflows' ? (
                                    <ButtonGroup aria-label="Workflow Creation Actions">
                                        <Button
                                            aria-label="Create Workflow"
                                            onClick={() => onCreateWorkflow(project.id)}
                                            size="sm"
                                            variant="secondary"
                                        >
                                            <PlusIcon />
                                            New Workflow
                                        </Button>

                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <Button
                                                    aria-label="More Workflow Creation Actions"
                                                    icon={<ChevronDownIcon />}
                                                    size="sm"
                                                    variant="secondary"
                                                >
                                                    <> </>
                                                </Button>
                                            </DropdownMenuTrigger>

                                            <DropdownMenuContent align="end">
                                                <DropdownMenuItem
                                                    aria-label="Import Workflow"
                                                    icon={<UploadIcon />}
                                                    label="Import Workflow"
                                                    onClick={() => onImportWorkflow(project.id)}
                                                />
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    </ButtonGroup>
                                ) : (
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <span>
                                                <Button
                                                    aria-label="New Configuration"
                                                    disabled={!project.published}
                                                    onClick={() => setConfigurationDialog({project})}
                                                    size="sm"
                                                    variant="secondary"
                                                >
                                                    <PlusIcon />
                                                    New Configuration
                                                </Button>
                                            </span>
                                        </TooltipTrigger>

                                        {!project.published && (
                                            <TooltipContent>Publish the project first</TooltipContent>
                                        )}
                                    </Tooltip>
                                )
                            }
                            activeTab={activeTabs[project.id] || 'workflows'}
                            configurationsContent={
                                <AutomationWorkflowProjectConfigurationList
                                    onEdit={(configuration) => setConfigurationDialog({configuration, project})}
                                    project={project}
                                />
                            }
                            onTabChange={(tab) =>
                                setActiveTabs((previousActiveTabs) => ({...previousActiveTabs, [project.id]: tab}))
                            }
                            workflowsContent={
                                <AutomationWorkflowProjectWorkflowList
                                    onDeleteWorkflow={onDeleteWorkflow}
                                    onEditWorkflow={onEditWorkflow}
                                    onSelectWorkflow={onSelectWorkflow}
                                    project={project}
                                />
                            }
                        />
                    </CollapsibleContent>
                </Collapsible>
            ))}

            {configurationDialog && (
                <AutomationWorkflowProjectConfigurationDialog
                    configuration={configurationDialog.configuration}
                    onClose={() => setConfigurationDialog(undefined)}
                    project={configurationDialog.project}
                />
            )}
        </div>
    );
};

export default AutomationWorkflowProjectList;
