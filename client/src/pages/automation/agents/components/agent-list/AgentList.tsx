import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Project} from '@/shared/middleware/automation/configuration';
import {AiAgent} from '@/shared/middleware/graphql';
import {useGetWorkspaceProjectDeploymentsQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import isInteractiveElementClick from '@/shared/util/interactive-element-utils';
import {ChevronDownIcon, RocketIcon} from 'lucide-react';
import {MouseEvent, useMemo, useRef, useState} from 'react';
import {Link} from 'react-router-dom';

import AgentListItem from './AgentListItem';

interface AgentListProps {
    agents: AiAgent[];
}

interface AgentProjectGroupI {
    agents: AiAgent[];
    project?: Project;
    projectId: string;
    projectName: string;
}

const AgentProjectGroup = ({projectGroup}: {projectGroup: AgentProjectGroupI}) => {
    const [isProjectDeploymentDialogOpen, setIsProjectDeploymentDialogOpen] = useState(false);

    const triggerRef = useRef<HTMLButtonElement | null>(null);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const project = projectGroup.project;

    const isPublished = !!(project?.lastPublishedDate && project?.lastProjectVersion);

    const projectDeploymentsQuery = useGetWorkspaceProjectDeploymentsQuery(
        {
            id: currentWorkspaceId ?? 0,
            projectId: project?.id ?? 0,
        },
        false
    );

    const handleRowClick = (event: React.MouseEvent) => {
        if (isProjectDeploymentDialogOpen) {
            return;
        }

        if (isInteractiveElementClick(event.target)) {
            return;
        }

        triggerRef.current?.click();
    };

    const handleProjectDeploymentDialogOpen = async (event: MouseEvent<HTMLButtonElement>) => {
        event.stopPropagation();

        if (!currentWorkspaceId || !project?.id) {
            return;
        }

        await projectDeploymentsQuery.refetch();
    };

    return (
        <Collapsible className="group mb-2 rounded border border-border/50" defaultOpen>
            <div
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 py-3 hover:bg-surface-neutral-primary-hover"
                onClick={handleRowClick}
            >
                <div className="flex-1">
                    <div className="flex min-h-8 items-center gap-1.5">
                        <Link
                            className="text-base font-semibold hover:underline"
                            to={
                                '/automation/projects/' +
                                projectGroup.projectId +
                                '/agents/' +
                                projectGroup.agents[0].id
                            }
                        >
                            {projectGroup.projectName || 'Untitled Project'}
                        </Link>
                    </div>

                    <div className="mt-2 flex min-h-6 items-center">
                        <CollapsibleTrigger
                            className="group/trigger flex items-center text-xs font-semibold text-muted-foreground"
                            ref={triggerRef}
                        >
                            <div className="mr-1">
                                {projectGroup.agents.length} {projectGroup.agents.length === 1 ? 'agent' : 'agents'}
                            </div>

                            <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                        </CollapsibleTrigger>
                    </div>
                </div>

                {project && (
                    <div className="flex flex-col items-end gap-y-2">
                        <div className="flex min-h-8 items-center gap-2">
                            {isPublished ? (
                                <>
                                    <Badge className="flex space-x-1" styleType="success-outline" weight="semibold">
                                        <span>V{project.lastProjectVersion! - 1}</span>

                                        <span>PUBLISHED</span>
                                    </Badge>

                                    <ProjectDeploymentDialog
                                        environmentEditable={true}
                                        onOpenChange={setIsProjectDeploymentDialogOpen}
                                        projectDeployment={{
                                            name: project.name,
                                            projectId: project.id,
                                        }}
                                        projectDeployments={projectDeploymentsQuery.data}
                                        projectDeploymentsLoading={projectDeploymentsQuery.isFetching}
                                        showTabs
                                        triggerNode={
                                            <Button
                                                className="hover:bg-surface-neutral-primary-hover"
                                                onClick={handleProjectDeploymentDialogOpen}
                                                size="sm"
                                                variant="outline"
                                            >
                                                <RocketIcon /> Deploy
                                            </Button>
                                        }
                                    />
                                </>
                            ) : (
                                <Badge className="flex space-x-1" styleType="secondary-filled" weight="semibold">
                                    <span>V{project.lastProjectVersion}</span>

                                    <span>{project.lastStatus}</span>
                                </Badge>
                            )}
                        </div>

                        <Tooltip>
                            <TooltipTrigger>
                                <div className="flex min-h-7 items-center text-xs text-muted-foreground">
                                    {project.lastPublishedDate
                                        ? `Published at ${new Date(project.lastPublishedDate).toLocaleDateString()} ${new Date(project.lastPublishedDate).toLocaleTimeString()}`
                                        : 'Not yet published'}
                                </div>
                            </TooltipTrigger>

                            <TooltipContent>Last Modified Date</TooltipContent>
                        </Tooltip>
                    </div>
                )}
            </div>

            <CollapsibleContent>
                <ul className="divide-y divide-stroke-neutral-primary px-2 pb-2">
                    {projectGroup.agents.map((agent) => (
                        <AgentListItem agent={agent} key={agent.id} />
                    ))}
                </ul>
            </CollapsibleContent>
        </Collapsible>
    );
};

const AgentList = ({agents}: AgentListProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: projects} = useGetWorkspaceProjectsQuery({id: currentWorkspaceId!}, !!currentWorkspaceId);

    const projectGroups = useMemo(() => {
        const projectsById = new Map((projects ?? []).map((project) => [String(project.id), project]));

        const groupsByProjectId = new Map<string, AgentProjectGroupI>();

        for (const agent of agents) {
            let projectGroup = groupsByProjectId.get(agent.projectId);

            if (!projectGroup) {
                const project = projectsById.get(agent.projectId);

                projectGroup = {
                    agents: [],
                    project,
                    projectId: agent.projectId,
                    projectName: project?.name ?? '',
                };

                groupsByProjectId.set(agent.projectId, projectGroup);
            }

            projectGroup.agents.push(agent);
        }

        return [...groupsByProjectId.values()].sort((a, b) => a.projectName.localeCompare(b.projectName));
    }, [agents, projects]);

    return (
        <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
            {projectGroups.map((projectGroup) => (
                <AgentProjectGroup key={projectGroup.projectId} projectGroup={projectGroup} />
            ))}
        </div>
    );
};

export default AgentList;
