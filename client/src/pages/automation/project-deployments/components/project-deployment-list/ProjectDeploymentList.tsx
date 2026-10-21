import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {useProjectDeploymentsEnabledStore} from '@/pages/automation/project-deployments/stores/useProjectDeploymentsEnabledStore';
import {Project, ProjectDeployment, Tag} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useCallback, useState} from 'react';

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
    const [collapsedProjectDeploymentIds, setCollapsedProjectDeploymentIds] = useState<Set<number>>(new Set());

    const projectDeploymentMap = useProjectDeploymentsEnabledStore(({projectDeploymentMap}) => projectDeploymentMap);

    const handleOpenChange = useCallback((open: boolean, projectDeploymentId: number) => {
        setCollapsedProjectDeploymentIds((prev) => {
            const projectDeploymentSet = new Set(prev);

            if (open) {
                projectDeploymentSet.delete(projectDeploymentId);
            } else {
                projectDeploymentSet.add(projectDeploymentId);
            }

            return projectDeploymentSet;
        });
    }, []);

    return (
        <>
            {projectDeployments.map((projectDeployment) => {
                const projectTagIds = projectDeployment.tags?.map((tag) => tag.id);

                if (!project || !project.id) {
                    return <></>;
                }

                return (
                    <Collapsible
                        className="group mb-2 rounded border border-border/50"
                        key={projectDeployment.id}
                        onOpenChange={(open) => handleOpenChange(open, projectDeployment.id!)}
                        open={!collapsedProjectDeploymentIds.has(projectDeployment.id!)}
                    >
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
                                    projectName={project.name}
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
