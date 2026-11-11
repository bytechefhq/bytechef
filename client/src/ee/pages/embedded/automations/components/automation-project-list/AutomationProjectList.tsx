import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {ButtonGroup} from '@/components/ui/button-group';
import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {
    AutomationProjectConfigurationsQuery,
    AutomationProjectTagsQuery,
    AutomationProjectsQuery,
} from '@/shared/middleware/graphql';
import {ChevronDownIcon, PlusIcon, UploadIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import AutomationProjectConfigurationDialog from '../automation-project-configuration-dialog/AutomationProjectConfigurationDialog';
import AutomationProjectConfigurationList from '../automation-project-configuration-list/AutomationProjectConfigurationList';
import AutomationProjectListItem from './AutomationProjectListItem';
import AutomationProjectListItemTabs, {AutomationProjectListItemTabType} from './AutomationProjectListItemTabs';
import AutomationProjectWorkflowList from './AutomationProjectWorkflowList';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];
type AutomationProjectConfigurationType =
    AutomationProjectConfigurationsQuery['automationProjectConfigurations'][number];
type EmbeddedTagType = AutomationProjectTagsQuery['automationProjectTags'][number];

interface AutomationProjectListProps {
    newlyCreatedProjectId?: string;
    onCreateWorkflow: (projectId: string) => void;
    onDeleteProject: (projectId: string) => void;
    onDeleteWorkflow: (workflowUuid: string) => void;
    onEditProject: (project: AutomationProjectType) => void;
    onEditWorkflow: (workflow: AutomationProjectType['workflowTemplates'][number]) => void;
    onImportWorkflow: (projectId: string) => void;
    onPublishProject: (projectId: string) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    onUpdateTags: (project: AutomationProjectType, tagNames: string[]) => void;
    projects: Array<AutomationProjectType>;
    tags: EmbeddedTagType[];
}

const AutomationProjectList = ({
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
}: AutomationProjectListProps) => {
    const [searchParams] = useSearchParams();

    const [activeTabs, setActiveTabs] = useState<Record<string, AutomationProjectListItemTabType>>(() => {
        const projectId = searchParams.get('projectId');

        return projectId && searchParams.get('tab') === 'configurations' ? {[projectId]: 'configurations'} : {};
    });
    const [configurationDialog, setConfigurationDialog] = useState<
        | {
              configuration?: AutomationProjectConfigurationType;
              project: AutomationProjectType;
              updateProjectVersion?: boolean;
          }
        | undefined
    >();
    const [openProjectIds, setOpenProjectIds] = useState<Set<string>>(() => {
        const projectId = searchParams.get('projectId');

        return new Set(projectId ? [projectId] : []);
    });

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
                    <AutomationProjectListItem
                        onCreateConfiguration={(selectedProject) => setConfigurationDialog({project: selectedProject})}
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

                    <CollapsibleContent className="mt-1 mr-3 mb-3 ml-11">
                        <AutomationProjectListItemTabs
                            actions={
                                (activeTabs[project.id] || 'workflows') === 'workflows'
                                    ? project.workflowTemplates.length > 0 && (
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
                                      )
                                    : project.published &&
                                      project.configurationCount > 0 && (
                                          <Button
                                              aria-label="New Configuration"
                                              onClick={() => setConfigurationDialog({project})}
                                              size="sm"
                                              variant="secondary"
                                          >
                                              <PlusIcon />
                                              New Configuration
                                          </Button>
                                      )
                            }
                            activeTab={activeTabs[project.id] || 'workflows'}
                            configurationsContent={
                                <AutomationProjectConfigurationList
                                    environmentId={DEVELOPMENT_ENVIRONMENT}
                                    onCreate={() => setConfigurationDialog({project})}
                                    onEdit={(configuration) => setConfigurationDialog({configuration, project})}
                                    onPublishProject={onPublishProject}
                                    onUpdateVersion={(configuration) =>
                                        setConfigurationDialog({configuration, project, updateProjectVersion: true})
                                    }
                                    project={project}
                                />
                            }
                            onTabChange={(tab) =>
                                setActiveTabs((previousActiveTabs) => ({...previousActiveTabs, [project.id]: tab}))
                            }
                            workflowsContent={
                                <AutomationProjectWorkflowList
                                    onCreateWorkflow={onCreateWorkflow}
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
                <AutomationProjectConfigurationDialog
                    configuration={configurationDialog.configuration}
                    onClose={() => setConfigurationDialog(undefined)}
                    project={configurationDialog.project}
                    updateProjectVersion={configurationDialog.updateProjectVersion}
                />
            )}
        </div>
    );
};

export default AutomationProjectList;
