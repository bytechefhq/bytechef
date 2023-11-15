import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {useProjectInstancesEnabledStore} from '@/pages/automation/project-instances/stores/useProjectInstancesEnabledStore';
import {useGetProjectInstanceTagsQuery} from '@/queries/projectInstanceTags.queries';
import {
    ProjectInstanceModel,
    ProjectModel,
} from 'middleware/helios/configuration';

import ProjectInstanceListItem from './ProjectInstanceListItem';
import ProjectInstanceWorkflowList from './ProjectInstanceWorkflowList';

const ProjectInstanceList = ({
    project,
    projectInstances,
}: {
    project: ProjectModel;
    projectInstances: ProjectInstanceModel[];
}) => {
    const {data: tags} = useGetProjectInstanceTagsQuery();
    const projectInstanceMap = useProjectInstancesEnabledStore(
        ({projectInstanceMap}) => projectInstanceMap
    );

    return (
        <div className="w-full px-2 2xl:mx-auto 2xl:w-4/5">
            {projectInstances.length > 0 && (
                <div className="mb-8">
                    <h3 className="mb-1 px-2 text-xl font-semibold">
                        {project.name}
                    </h3>

                    {projectInstances.map((projectInstance) => {
                        const projectTagIds = projectInstance.tags?.map(
                            (tag) => tag.id
                        );

                        if (!project.id) {
                            return;
                        }

                        return (
                            <Collapsible
                                className="data-[state=closed]:border-b data-[state=closed]:border-b-gray-100"
                                key={projectInstance.id}
                            >
                                <div className="w-full rounded-md px-2 py-5 hover:bg-gray-50">
                                    <ProjectInstanceListItem
                                        key={projectInstance.id}
                                        project={project}
                                        projectInstance={projectInstance}
                                        remainingTags={tags?.filter(
                                            (tag) =>
                                                !projectTagIds?.includes(tag.id)
                                        )}
                                    />
                                </div>

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
                                        projectInstanceId={projectInstance.id!}
                                        projectInstanceWorkflows={
                                            projectInstance.projectInstanceWorkflows
                                        }
                                    />
                                </CollapsibleContent>
                            </Collapsible>
                        );
                    })}
                </div>
            )}
        </div>
    );
};
export default ProjectInstanceList;
