import EmptyList from '@/components/EmptyList';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import {ProjectDeploymentWorkflow} from '@/shared/middleware/automation/configuration';
import {BotIcon, ChevronDownIcon} from 'lucide-react';
import {useMemo} from 'react';

import ProjectDeploymentAgentListItem, {ProjectDeploymentAgentType} from './ProjectDeploymentAgentListItem';

interface ProjectDeploymentAgentListProps {
    agentDeployments: ProjectDeploymentAgentType[];
    projectDeploymentWorkflows: ProjectDeploymentWorkflow[];
}

const ProjectDeploymentAgentList = ({
    agentDeployments,
    projectDeploymentWorkflows,
}: ProjectDeploymentAgentListProps) => {
    const {agents} = useAgents();

    const agentRows = useMemo(
        () =>
            agentDeployments.map((agentDeployment) => {
                const agentWorkflowIds = new Set(agentDeployment.workflows.map((workflow) => workflow.workflowId));

                return {
                    agent: agents.find((agent) => agent.id === agentDeployment.agentId),
                    agentDeployment,
                    projectDeploymentWorkflow: projectDeploymentWorkflows.find(
                        (projectDeploymentWorkflow) =>
                            projectDeploymentWorkflow.workflowId != null &&
                            agentWorkflowIds.has(projectDeploymentWorkflow.workflowId)
                    ),
                };
            }),
        [agentDeployments, agents, projectDeploymentWorkflows]
    );

    const enabledAgentRows = agentRows.filter((agentRow) => agentRow.projectDeploymentWorkflow?.enabled);
    const disabledAgentRows = agentRows.filter((agentRow) => !agentRow.projectDeploymentWorkflow?.enabled);

    if (agentDeployments.length === 0) {
        return (
            <div className="flex justify-center py-8">
                <EmptyList
                    icon={<BotIcon className="size-24 text-stroke-neutral-tertiary" />}
                    message="This deployment has no agents."
                    title="No Agents"
                />
            </div>
        );
    }

    return (
        <div className="pt-3">
            {enabledAgentRows.length === 0 ? (
                <p className="py-4 pl-3 text-sm text-muted-foreground">
                    No enabled agents. Enable an agent in the project to run it in this deployment.
                </p>
            ) : (
                <ul className="divide-y divide-stroke-neutral-primary">
                    {enabledAgentRows.map((agentRow) => (
                        <ProjectDeploymentAgentListItem
                            agent={agentRow.agent}
                            agentDeployment={agentRow.agentDeployment}
                            key={agentRow.agentDeployment.agentId}
                            projectDeploymentWorkflow={agentRow.projectDeploymentWorkflow}
                        />
                    ))}
                </ul>
            )}

            {disabledAgentRows.length > 0 && (
                <Collapsible className="group">
                    <CollapsibleTrigger className="flex w-full items-center space-x-2 rounded-md p-2 px-3 hover:bg-surface-neutral-primary-hover [&[data-state=open]>svg]:rotate-180">
                        <h3 className="flex justify-start text-sm text-muted-foreground">Disabled Agents</h3>

                        <ChevronDownIcon className="size-4 shrink-0 transition-transform duration-300" />
                    </CollapsibleTrigger>

                    <CollapsibleContent>
                        <ul className="divide-y divide-stroke-neutral-primary">
                            {disabledAgentRows.map((agentRow) => (
                                <ProjectDeploymentAgentListItem
                                    agent={agentRow.agent}
                                    agentDeployment={agentRow.agentDeployment}
                                    key={agentRow.agentDeployment.agentId}
                                    projectDeploymentWorkflow={agentRow.projectDeploymentWorkflow}
                                />
                            ))}
                        </ul>
                    </CollapsibleContent>
                </Collapsible>
            )}
        </div>
    );
};

export default ProjectDeploymentAgentList;
