import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import ProjectListItem from '@/pages/automation/projects/components/project-list/ProjectListItem';
import {ProjectGitConfigurationI} from '@/shared/edition/project-git/projectGitApi';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useEffect, useMemo, useState} from 'react';

import ProjectAgentCreationActions from '../project-agent-list/ProjectAgentCreationActions';
import ProjectAgentList from '../project-agent-list/ProjectAgentList';
import ProjectWorkflowList from '../project-workflow-list/ProjectWorkflowList';

export type ProjectListTabType = 'agents' | 'workflows';

const ProjectList = ({
    componentDefinitions,
    defaultActiveTab = 'workflows',
    isRefetchingProjects,
    newlyCreatedProjectId,
    projectGitConfigurations,
    projects,
    tags,
    taskDispatcherDefinitions,
}: {
    componentDefinitions?: ComponentDefinitionBasic[];
    defaultActiveTab?: ProjectListTabType;
    isRefetchingProjects?: boolean;
    newlyCreatedProjectId?: number;
    projectGitConfigurations: ProjectGitConfigurationI[];
    projects: Project[];
    tags: Tag[];
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}) => {
    const [activeTabByProjectId, setActiveTabByProjectId] = useState<Record<number, ProjectListTabType>>({});
    const [openCollapsibles, setOpenCollapsibles] = useState<Set<number>>(new Set());

    const {agents} = useAgents();

    const agentCountsByProjectId = useMemo(() => {
        const counts = new Map<number, number>();

        for (const agent of agents) {
            const agentProjectId = +agent.projectId;

            counts.set(agentProjectId, (counts.get(agentProjectId) ?? 0) + 1);
        }

        return counts;
    }, [agents]);

    useEffect(() => {
        setActiveTabByProjectId({});
    }, [defaultActiveTab]);

    useEffect(() => {
        if (newlyCreatedProjectId) {
            setOpenCollapsibles((prev) => new Set([...prev, newlyCreatedProjectId]));
        }
    }, [newlyCreatedProjectId]);

    return (
        <div className="w-full divide-y divide-border/50 self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
            {projects.map((project) => {
                const projectTagIds = project.tags?.map((tag) => tag.id);
                const workflowCount = project.projectWorkflowIds?.length || 0;
                const agentCount = agentCountsByProjectId.get(project.id!) || 0;
                const activeTab = activeTabByProjectId[project.id!] || defaultActiveTab;

                return (
                    <Collapsible
                        className="group mb-2 rounded border border-border/50"
                        key={project.id}
                        onOpenChange={(open) => {
                            setOpenCollapsibles((prev) => {
                                const newSet = new Set(prev);
                                if (open) {
                                    newSet.add(project.id!);
                                } else {
                                    newSet.delete(project.id!);
                                }
                                return newSet;
                            });
                        }}
                        open={openCollapsibles.has(project.id!)}
                    >
                        <ProjectListItem
                            key={project.id}
                            project={project}
                            projectGitConfiguration={projectGitConfigurations.find(
                                (projectGitConfiguration) => projectGitConfiguration.projectId === project.id
                            )}
                            remainingTags={tags?.filter((tag) => !projectTagIds?.includes(tag.id))}
                        />

                        <CollapsibleContent>
                            <Tabs
                                className="px-3 py-2"
                                onValueChange={(value) =>
                                    setActiveTabByProjectId((previous) => ({
                                        ...previous,
                                        [project.id!]: value as ProjectListTabType,
                                    }))
                                }
                                value={activeTab}
                            >
                                <div className="flex items-center justify-between">
                                    <TabsList>
                                        <TabsTrigger value="workflows">Workflows ({workflowCount})</TabsTrigger>

                                        <TabsTrigger value="agents">Agents ({agentCount})</TabsTrigger>
                                    </TabsList>

                                    <div onClick={(event) => event.stopPropagation()}>
                                        {activeTab === 'agents' && agentCount > 0 && (
                                            <ProjectAgentCreationActions placement="tabRow" project={project} />
                                        )}
                                    </div>
                                </div>

                                <TabsContent value="workflows">
                                    <ProjectWorkflowList
                                        componentDefinitions={componentDefinitions}
                                        project={project}
                                        queryEnabled={!isRefetchingProjects}
                                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                                    />
                                </TabsContent>

                                <TabsContent value="agents">
                                    <ProjectAgentList project={project} />
                                </TabsContent>
                            </Tabs>
                        </CollapsibleContent>
                    </Collapsible>
                );
            })}
        </div>
    );
};
export default ProjectList;
