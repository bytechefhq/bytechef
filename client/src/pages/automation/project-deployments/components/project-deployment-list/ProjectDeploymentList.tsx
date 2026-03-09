import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {useProjectDeploymentsEnabledStore} from '@/pages/automation/project-deployments/stores/useProjectDeploymentsEnabledStore';
import {Project, ProjectDeployment, Tag} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';

import ProjectDeploymentWorkflowList from '../project-deployment-workflow-list/ProjectDeploymentWorkflowList';
import ProjectDeploymentListItem from './ProjectDeploymentListItem';

interface ProjectDeploymentListProps {
    componentDefinitions?: ComponentDefinitionBasic[];
    project: Project;
    projectDeployments: ProjectDeployment[];
    tags: Tag[];
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}

const ProjectDeploymentList = ({
    componentDefinitions,
    project,
    projectDeployments,
    tags,
    taskDispatcherDefinitions,
}: ProjectDeploymentListProps) => {
    const projectDeploymentMap = useProjectDeploymentsEnabledStore(({projectDeploymentMap}) => projectDeploymentMap);

    return (
        <>
            {projectDeployments.map((projectDeployment) => {
                const projectTagIds = projectDeployment.tags?.map((tag) => tag.id);

                if (!project || !project.id) {
                    return <></>;
                }

                return (
                    <Collapsible className="group" key={projectDeployment.id}>
                        <ProjectDeploymentListItem
                            key={projectDeployment.id}
                            projectDeployment={projectDeployment}
                            remainingTags={tags?.filter((tag) => !projectTagIds?.includes(tag.id))}
                        />

                        {!!projectDeployment.projectDeploymentWorkflows?.length && (
                            <CollapsibleContent>
                                <ProjectDeploymentWorkflowList
                                    componentDefinitions={componentDefinitions}
                                    environmentId={projectDeployment.environmentId!}
                                    projectDeploymentEnabled={
                                        projectDeploymentMap.has(projectDeployment.id!)
                                            ? projectDeploymentMap.get(projectDeployment.id!)!
                                            : projectDeployment.enabled!
                                    }
                                    projectDeploymentId={projectDeployment.id!}
                                    projectDeploymentWorkflows={projectDeployment.projectDeploymentWorkflows}
                                    projectId={project.id}
                                    projectVersion={projectDeployment.projectVersion!}
                                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                                />
                            </CollapsibleContent>
                        )}
                    </Collapsible>
                );
            })}
        </>
    );
};

export default ProjectDeploymentList;
