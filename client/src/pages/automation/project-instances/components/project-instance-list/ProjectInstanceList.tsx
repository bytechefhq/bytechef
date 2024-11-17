import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {useProjectInstancesEnabledStore} from '@/pages/automation/project-instances/stores/useProjectInstancesEnabledStore';
import {Project, ProjectInstance, Tag} from '@/shared/middleware/automation/configuration';

import ProjectInstanceWorkflowList from '../project-instance-workflow-list/ProjectInstanceWorkflowList';
import ProjectInstanceListItem from './ProjectInstanceListItem';

const ProjectInstanceList = ({
    project,
    projectInstances,
    tags,
}: {
    project: Project;
    projectInstances: ProjectInstance[];
    tags: Tag[];
}) => {
    const projectInstanceMap = useProjectInstancesEnabledStore(({projectInstanceMap}) => projectInstanceMap);

    return (
        <>
            {projectInstances.map((projectInstance) => {
                const projectTagIds = projectInstance.tags?.map((tag) => tag.id);

                if (!project || !project.id) {
                    return <></>;
                }

                return (
                    <Collapsible className="group" key={projectInstance.id}>
                        <ProjectInstanceListItem
                            key={projectInstance.id}
                            projectInstance={projectInstance}
                            remainingTags={tags?.filter((tag) => !projectTagIds?.includes(tag.id))}
                        />

                        {!!projectInstance.projectInstanceWorkflows?.length && (
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
                        )}
                    </Collapsible>
                );
            })}
        </>
    );
};

export default ProjectInstanceList;
