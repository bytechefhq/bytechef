import EmptyList from '@/components/EmptyList';
import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import useAgentDeployments from '@/pages/automation/project-deployments/hooks/useAgentDeployments';
import {useProjectDeploymentsEnabledStore} from '@/pages/automation/project-deployments/stores/useProjectDeploymentsEnabledStore';
import {Project, ProjectDeployment, Tag} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {WorkflowIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useState} from 'react';

import ProjectDeploymentAgentList from '../project-deployment-agent-list/ProjectDeploymentAgentList';
import {ProjectDeploymentAgentType} from '../project-deployment-agent-list/ProjectDeploymentAgentListItem';
import ProjectDeploymentWorkflowList from '../project-deployment-workflow-list/ProjectDeploymentWorkflowList';
import ProjectDeploymentListItem from './ProjectDeploymentListItem';

export type ProjectDeploymentListTabType = 'agents' | 'workflows';

interface ProjectDeploymentListProps {
    componentDefinitions?: ComponentDefinitionBasic[];
    defaultActiveTab?: ProjectDeploymentListTabType;
    hideOpenInProject?: boolean;
    newlyCreatedDeploymentId?: number;
    project: Project;
    projectDeployments: ProjectDeployment[];
    showEnvironment?: boolean;
    tags: Tag[];
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}

const ProjectDeploymentList = ({
    componentDefinitions,
    defaultActiveTab = 'workflows',
    hideOpenInProject,
    newlyCreatedDeploymentId,
    project,
    projectDeployments,
    showEnvironment,
    tags,
    taskDispatcherDefinitions,
}: ProjectDeploymentListProps) => {
    const [activeTabByProjectDeploymentId, setActiveTabByProjectDeploymentId] = useState<
        Record<number, ProjectDeploymentListTabType>
    >({});
    const [openCollapsibles, setOpenCollapsibles] = useState<Set<number>>(new Set());

    const projectDeploymentMap = useProjectDeploymentsEnabledStore(({projectDeploymentMap}) => projectDeploymentMap);

    const {agents} = useAgents();
    const {agentDeployments} = useAgentDeployments();

    const agentWorkflowUuids = useMemo(() => new Set(agents.map((agent) => agent.projectWorkflowUuid)), [agents]);

    const agentDeploymentsByProjectDeploymentId = useMemo(() => {
        const agentDeploymentsMap = new Map<number, ProjectDeploymentAgentType[]>();

        for (const agentDeployment of agentDeployments) {
            const projectDeploymentId = +agentDeployment.id;

            agentDeploymentsMap.set(projectDeploymentId, [
                ...(agentDeploymentsMap.get(projectDeploymentId) || []),
                agentDeployment,
            ]);
        }

        return agentDeploymentsMap;
    }, [agentDeployments]);

    const handleOpenChange = useCallback((open: boolean, projectDeploymentId: number) => {
        setOpenCollapsibles((prev) => {
            const projectDeploymentSet = new Set(prev);

            if (open) {
                projectDeploymentSet.add(projectDeploymentId);
            } else {
                projectDeploymentSet.delete(projectDeploymentId);
            }
            return projectDeploymentSet;
        });
    }, []);

    useEffect(() => {
        if (newlyCreatedDeploymentId) {
            setOpenCollapsibles((prev) => new Set([...prev, newlyCreatedDeploymentId]));
        }
    }, [newlyCreatedDeploymentId]);

    useEffect(() => {
        setActiveTabByProjectDeploymentId({});
    }, [defaultActiveTab]);

    return (
        <>
            {projectDeployments.map((projectDeployment) => {
                const projectTagIds = projectDeployment.tags?.map((tag) => tag.id);

                if (!project || !project.id) {
                    return <></>;
                }

                const projectDeploymentAgents = agentDeploymentsByProjectDeploymentId.get(projectDeployment.id!) || [];

                const agentWorkflowIds = new Set(
                    projectDeploymentAgents.flatMap((agentDeployment) =>
                        agentDeployment.workflows.map((workflow) => workflow.workflowId)
                    )
                );

                const projectDeploymentWorkflows = projectDeployment.projectDeploymentWorkflows || [];

                const ordinaryProjectDeploymentWorkflows = projectDeploymentWorkflows.filter(
                    (projectDeploymentWorkflow) =>
                        !agentWorkflowIds.has(projectDeploymentWorkflow.workflowId!) &&
                        !(
                            projectDeploymentWorkflow.workflowUuid &&
                            agentWorkflowUuids.has(projectDeploymentWorkflow.workflowUuid)
                        )
                );

                const activeTab = activeTabByProjectDeploymentId[projectDeployment.id!] || defaultActiveTab;

                return (
                    <Collapsible
                        className="group mb-2 rounded border border-border/50"
                        key={projectDeployment.id}
                        onOpenChange={(open) => handleOpenChange(open, projectDeployment.id!)}
                        open={openCollapsibles.has(projectDeployment.id!)}
                    >
                        <ProjectDeploymentListItem
                            hideOpenInProject={hideOpenInProject}
                            key={projectDeployment.id}
                            projectDeployment={projectDeployment}
                            remainingTags={tags?.filter((tag) => !projectTagIds?.includes(tag.id))}
                            showEnvironment={showEnvironment}
                        />

                        <CollapsibleContent>
                            <Tabs
                                className="px-3 py-2"
                                onValueChange={(value) =>
                                    setActiveTabByProjectDeploymentId((previous) => ({
                                        ...previous,
                                        [projectDeployment.id!]: value as ProjectDeploymentListTabType,
                                    }))
                                }
                                value={activeTab}
                            >
                                <TabsList>
                                    <TabsTrigger value="workflows">
                                        Workflows ({ordinaryProjectDeploymentWorkflows.length})
                                    </TabsTrigger>

                                    <TabsTrigger value="agents">Agents ({projectDeploymentAgents.length})</TabsTrigger>
                                </TabsList>

                                <TabsContent value="workflows">
                                    {ordinaryProjectDeploymentWorkflows.length > 0 ? (
                                        <ProjectDeploymentWorkflowList
                                            componentDefinitions={componentDefinitions}
                                            environmentId={projectDeployment.environmentId!}
                                            hideOpenInProject={hideOpenInProject}
                                            projectDeploymentEnabled={
                                                projectDeploymentMap.has(projectDeployment.id!)
                                                    ? projectDeploymentMap.get(projectDeployment.id!)!
                                                    : projectDeployment.enabled!
                                            }
                                            projectDeploymentId={projectDeployment.id!}
                                            projectDeploymentWorkflows={ordinaryProjectDeploymentWorkflows}
                                            projectId={project.id}
                                            projectName={project.name}
                                            projectVersion={projectDeployment.projectVersion!}
                                            taskDispatcherDefinitions={taskDispatcherDefinitions}
                                        />
                                    ) : (
                                        <div className="flex justify-center py-8">
                                            <EmptyList
                                                icon={<WorkflowIcon className="size-24 text-stroke-neutral-tertiary" />}
                                                message="This deployment has no workflows."
                                                title="No Workflows"
                                            />
                                        </div>
                                    )}
                                </TabsContent>

                                <TabsContent value="agents">
                                    <ProjectDeploymentAgentList
                                        agentDeployments={projectDeploymentAgents}
                                        projectDeploymentWorkflows={projectDeploymentWorkflows}
                                    />
                                </TabsContent>
                            </Tabs>
                        </CollapsibleContent>
                    </Collapsible>
                );
            })}
        </>
    );
};

export default ProjectDeploymentList;
