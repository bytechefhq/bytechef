import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {useProjectInstancesEnabledStore} from '@/pages/automation/project-instances/stores/useProjectInstancesEnabledStore';
import {
    ProjectInstanceModel,
    ProjectModel,
    TagModel,
} from 'middleware/helios/configuration';

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
    const projectInstanceMap = useProjectInstancesEnabledStore(
        ({projectInstanceMap}) => projectInstanceMap
    );

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {projectInstances.length > 0 && (
                <>
                    <h3 className="mb-1 px-2 text-xl font-semibold">
                        {project.name}
                    </h3>

                    <div className="w-full divide-y divide-gray-100">
                        {projectInstances.map((projectInstance) => {
                            const projectTagIds = projectInstance.tags?.map(
                                (tag) => tag.id
                            );

                            if (!project.id) {
                                return;
                            }

                            return (
                                <Collapsible key={projectInstance.id}>
                                    <ProjectInstanceListItem
                                        key={projectInstance.id}
                                        project={project}
                                        projectInstance={projectInstance}
                                        remainingTags={tags?.filter(
                                            (tag) =>
                                                !projectTagIds?.includes(tag.id)
                                        )}
                                    />

                                    <CollapsibleContent>
                                        <ProjectInstanceWorkflowList
                                            projectId={project.id}
                                            projectInstanceEnabled={
                                                projectInstanceMap.has(
                                                    projectInstance.id!
                                                )
                                                    ? projectInstanceMap.get(
                                                          projectInstance.id!
                                                      )!
                                                    : projectInstance.enabled!
                                            }
                                            projectInstanceId={
                                                projectInstance.id!
                                            }
                                            projectInstanceWorkflows={
                                                projectInstance.projectInstanceWorkflows
                                            }
                                        />
                                    </CollapsibleContent>
                                </Collapsible>
                            );
                        })}
                    </div>
                </>
            )}
        </div>
    );
};
export default ProjectInstanceList;
