import {Collapsible} from '@/components/ui/collapsible';
import ProjectDeploymentsPanel from '@/pages/automation/project/components/ProjectDeploymentsPanel';
import ProjectListItem from '@/pages/automation/projects/components/project-list/ProjectListItem';
import {ProjectListItemTabType} from '@/pages/automation/projects/components/project-list/ProjectListItemTabs';
import {ProjectGitConfigurationI} from '@/shared/edition/project-git/projectGitApi';
import {Project, Tag} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useCallback, useEffect, useRef, useState} from 'react';
import {useLocation, useSearchParams} from 'react-router-dom';

import ProjectWorkflowList from '../project-workflow-list/ProjectWorkflowList';

interface OpenProjectI {
    expandedProjectDeploymentId?: number;
    tab: ProjectListItemTabType;
}

const getInitialOpenProjects = (searchParams: URLSearchParams, locationState: unknown) => {
    const openProjects = new Map<number, OpenProjectI>();

    const projectId = Number(searchParams.get('projectId'));

    if (projectId && searchParams.get('tab') === 'deployments') {
        openProjects.set(projectId, {
            expandedProjectDeploymentId: (locationState as {projectDeploymentId?: number} | null)?.projectDeploymentId,
            tab: 'deployments',
        });
    }

    return openProjects;
};

const ProjectList = ({
    componentDefinitions,
    isRefetchingProjects,
    newlyCreatedProjectId,
    projectGitConfigurations,
    projects,
    tags,
    taskDispatcherDefinitions,
}: {
    componentDefinitions?: ComponentDefinitionBasic[];
    isRefetchingProjects?: boolean;
    newlyCreatedProjectId?: number;
    projectGitConfigurations: ProjectGitConfigurationI[];
    projects: Project[];
    tags: Tag[];
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}) => {
    const [searchParams, setSearchParams] = useSearchParams();

    const location = useLocation();

    const [openProjects, setOpenProjects] = useState<Map<number, OpenProjectI>>(() =>
        getInitialOpenProjects(searchParams, location.state)
    );

    const scrollToProjectIdRef = useRef<number | undefined>(openProjects.keys().next().value);

    const handleTabChange = useCallback(
        (projectId: number, tab?: ProjectListItemTabType, expandedProjectDeploymentId?: number) => {
            setOpenProjects((previousOpenProjects) => {
                const newOpenProjects = new Map(previousOpenProjects);

                if (tab) {
                    newOpenProjects.set(projectId, {expandedProjectDeploymentId, tab});
                } else {
                    newOpenProjects.delete(projectId);
                }

                return newOpenProjects;
            });
        },
        []
    );

    useEffect(() => {
        if (!searchParams.has('projectId') && !searchParams.has('tab')) {
            return;
        }

        const remainingSearchParams = new URLSearchParams(searchParams);

        remainingSearchParams.delete('projectId');
        remainingSearchParams.delete('tab');

        setSearchParams(remainingSearchParams, {replace: true});
    }, [searchParams, setSearchParams]);

    useEffect(() => {
        const scrollToProjectId = scrollToProjectIdRef.current;

        if (scrollToProjectId === undefined || !projects.some((project) => project.id === scrollToProjectId)) {
            return;
        }

        scrollToProjectIdRef.current = undefined;

        document.querySelector(`[data-project-id="${scrollToProjectId}"]`)?.scrollIntoView({block: 'start'});
    }, [projects]);

    useEffect(() => {
        if (newlyCreatedProjectId) {
            handleTabChange(newlyCreatedProjectId, 'workflows');
        }
    }, [handleTabChange, newlyCreatedProjectId]);

    return (
        <div className="w-full divide-y divide-border/50 self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
            {projects.map((project) => {
                const openProject = openProjects.get(project.id!);
                const projectTagIds = project.tags?.map((tag) => tag.id);

                return (
                    <Collapsible
                        className="group/project mb-2 rounded border border-border/50"
                        data-project-id={project.id}
                        key={project.id}
                        onOpenChange={(open) => handleTabChange(project.id!, open ? 'workflows' : undefined)}
                        open={!!openProject}
                    >
                        <ProjectListItem
                            activeTab={openProject?.tab}
                            deploymentsContent={
                                <ProjectDeploymentsPanel
                                    enabled
                                    expandAll
                                    expandedProjectDeploymentId={openProject?.expandedProjectDeploymentId}
                                    onProjectDeploymentSaved={(projectDeploymentId) =>
                                        handleTabChange(project.id!, 'deployments', projectDeploymentId)
                                    }
                                    project={project}
                                />
                            }
                            key={project.id}
                            onTabChange={(tab, expandedProjectDeploymentId) =>
                                handleTabChange(project.id!, tab, expandedProjectDeploymentId)
                            }
                            project={project}
                            projectGitConfiguration={projectGitConfigurations.find(
                                (projectGitConfiguration) => projectGitConfiguration.projectId === project.id
                            )}
                            remainingTags={tags?.filter((tag) => !projectTagIds?.includes(tag.id))}
                            workflowsContent={
                                <ProjectWorkflowList
                                    componentDefinitions={componentDefinitions}
                                    project={project}
                                    queryEnabled={!isRefetchingProjects}
                                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                                />
                            }
                        />
                    </Collapsible>
                );
            })}
        </div>
    );
};

export default ProjectList;
