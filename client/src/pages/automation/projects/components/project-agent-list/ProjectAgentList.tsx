import EmptyList from '@/components/EmptyList';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import ProjectAgentCreationActions from '@/pages/automation/projects/components/project-agent-list/ProjectAgentCreationActions';
import ProjectAgentListItem from '@/pages/automation/projects/components/project-agent-list/ProjectAgentListItem';
import {Project} from '@/shared/middleware/automation/configuration';
import {BotIcon} from 'lucide-react';
import {useMemo} from 'react';

interface ProjectAgentListProps {
    project: Project;
}

const ProjectAgentList = ({project}: ProjectAgentListProps) => {
    const {agents} = useAgents();

    const projectAgents = useMemo(
        () => agents.filter((agent) => +agent.projectId === project.id),
        [agents, project.id]
    );

    return (
        <div className="pt-3">
            {projectAgents.length > 0 ? (
                <ul className="divide-y divide-stroke-neutral-primary">
                    {projectAgents.map((agent) => (
                        <ProjectAgentListItem agent={agent} key={agent.id} />
                    ))}
                </ul>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={<ProjectAgentCreationActions placement="emptyState" project={project} />}
                        icon={<BotIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new agent."
                        title="No Agents"
                    />
                </div>
            )}
        </div>
    );
};

export default ProjectAgentList;
