import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {useProjectInstancesEnabledStore} from '@/pages/automation/project-instances/stores/useProjectInstancesEnabledStore';
import {ProjectInstanceModel, ProjectModel, TagModel} from 'middleware/automation/configuration';

import ProjectInstanceListItem from './ProjectInstanceListItem';
import ProjectInstanceWorkflowList from './ProjectInstanceWorkflowList';

const ProjectInstanceList = ({
    project,
    projectInstances,
    tags,
}: {
    project: ProjectModel;
    projectInstances: ProjectInstanceModel[];
    tags: TagModel[];
}) => {
    const projectInstanceMap = useProjectInstancesEnabledStore(({projectInstanceMap}) => projectInstanceMap);

    return (
        <>
            {projectInstances.length > 0 && (
                <div className="w-full">
                    {projectInstances.map((projectInstance) => {
                        const projectTagIds = projectInstance.tags?.map((tag) => tag.id);

                        if (!project || !project.id) {
                            return;
                        }

                        return (
                            <Collapsible className="group" key={projectInstance.id}>
                                <ProjectInstanceListItem
                                    key={projectInstance.id}
                                    projectInstance={projectInstance}
                                    remainingTags={tags?.filter((tag) => !projectTagIds?.includes(tag.id))}
                                />

                                <CollapsibleContent>
                                    <ProjectInstanceWorkflowList
                                        projectId={project.id}
                                        projectInstanceEnabled={
                                            projectInstanceMap.has(projectInstance.id!)
                                                ? projectInstanceMap.get(projectInstance.id!)!
                                                : projectInstance.enabled!
                                        }
                                        projectInstanceId={projectInstance.id!}
                                        projectInstanceWorkflows={projectInstance.projectInstanceWorkflows}
                                        projectVersion={projectInstance.projectVersion!}
                                    />
                                </CollapsibleContent>
                            </Collapsible>
                        );
                    })}
                </div>
            )}
        </>
    );
};

export default ProjectInstanceList;
