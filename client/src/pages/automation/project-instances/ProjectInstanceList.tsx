import {useProjectInstancesEnabledStore} from '@/pages/automation/project-instances/stores/useProjectInstancesEnabledStore';
import {useGetProjectInstanceTagsQuery} from '@/queries/projects.queries';
import {
    Accordion,
    AccordionContent,
    AccordionItem,
} from '@radix-ui/react-accordion';
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
                <Accordion type="multiple" className="mb-8">
                    <h3 className="mb-1 px-2 text-xl font-semibold text-gray-900">
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
                            <AccordionItem
                                value={projectInstance.id!.toString()}
                                key={projectInstance.id}
                                className="data-[state=closed]:border-b data-[state=closed]:border-b-gray-100"
                            >
                                <div className="w-full rounded-md px-2 py-5 hover:bg-gray-50">
                                    <ProjectInstanceListItem
                                        projectInstance={projectInstance}
                                        key={projectInstance.id}
                                        remainingTags={tags?.filter(
                                            (tag) =>
                                                !projectTagIds?.includes(tag.id)
                                        )}
                                        project={project}
                                    />
                                </div>

                                <AccordionContent>
                                    <ProjectInstanceWorkflowList
                                        projectId={project.id}
                                        projectInstanceId={projectInstance.id!}
                                        projectInstanceEnabled={
                                            projectInstanceMap.has(
                                                projectInstance.id!
                                            )
                                                ? projectInstanceMap.get(
                                                      projectInstance.id!
                                                  )!
                                                : projectInstance.enabled!
                                        }
                                        projectInstanceWorkflows={
                                            projectInstance.projectInstanceWorkflows
                                        }
                                    />
                                </AccordionContent>
                            </AccordionItem>
                        );
                    })}
                </Accordion>
            )}
        </div>
    );
};
export default ProjectInstanceList;
