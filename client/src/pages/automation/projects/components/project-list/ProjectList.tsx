import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {ProjectGitConfiguration} from '@/ee/shared/middleware/automation/configuration';
import ProjectListItem from '@/pages/automation/projects/components/project-list/ProjectListItem';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useEffect, useState} from 'react';

import ProjectWorkflowList from '../project-workflow-list/ProjectWorkflowList';

const ProjectList = ({
    componentDefinitions,
    isRefetchingProjects,
    newlyCreatedProjectId,
    projectGitConfigurations,
    projects,
    tags,
    taskDispatcherDefinitions,
}: {
    componentDefinitions?: ComponentDefinitionBasic[];
    isRefetchingProjects?: boolean;
    newlyCreatedProjectId?: number;
    projectGitConfigurations: ProjectGitConfiguration[];
    projects: Project[];
    tags: Tag[];
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}) => {
    const [openCollapsibles, setOpenCollapsibles] = useState<Set<number>>(new Set());

    useEffect(() => {
        if (newlyCreatedProjectId) {
            setOpenCollapsibles((prev) => new Set([...prev, newlyCreatedProjectId]));
        }
    }, [newlyCreatedProjectId]);

    return (
        <div className="w-full divide-y divide-border/50 px-4 3xl:mx-auto 3xl:w-4/5">
            {projects.map((project) => {
                const projectTagIds = project.tags?.map((tag) => tag.id);

                return (
                    <Collapsible
                        className="group"
                        key={project.id}
                        onOpenChange={(open) => {
                            setOpenCollapsibles((prev) => {
                                const newSet = new Set(prev);
                                if (open) {
                                    newSet.add(project.id!);
                                } else {
                                    newSet.delete(project.id!);
                                }
                                return newSet;
                            });
                        }}
                        open={openCollapsibles.has(project.id!)}
                    >
                        <ProjectListItem
                            key={project.id}
                            project={project}
                            projectGitConfiguration={projectGitConfigurations.find(
                                (projectGitConfiguration) => projectGitConfiguration.projectId === project.id
                            )}
                            remainingTags={tags?.filter((tag) => !projectTagIds?.includes(tag.id))}
                        />

                        <CollapsibleContent>
                            <ProjectWorkflowList
                                componentDefinitions={componentDefinitions}
                                project={project}
                                queryEnabled={!isRefetchingProjects}
                                taskDispatcherDefinitions={taskDispatcherDefinitions}
                            />
                        </CollapsibleContent>
                    </Collapsible>
                );
            })}
        </div>
    );
};
export default ProjectList;
