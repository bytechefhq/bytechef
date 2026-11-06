import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import AutomationWorkflowExecutionsSheet from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-executions-sheet/AutomationWorkflowExecutionsSheet';
import AutomationWorkflowProjectConfigurationDialog from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-configuration-dialog/AutomationWorkflowProjectConfigurationDialog';
import AutomationWorkflowProjectConfigurationList from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-configuration-list/AutomationWorkflowProjectConfigurationList';
import useOpenInEmbeddedEditor from '@/ee/pages/embedded/shared/useOpenInEmbeddedEditor';
import ListGroupHeader from '@/shared/components/ListGroupHeader';
import OpenInButton from '@/shared/components/OpenInButton';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import LeftSidebarFilterNav from '@/shared/layout/LeftSidebarFilterNav';
import {
    AutomationWorkflowProjectConfigurationsQuery,
    AutomationWorkflowProjectsQuery,
    useAutomationWorkflowProjectsQuery,
} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {FolderIcon, SlidersHorizontalIcon} from 'lucide-react';
import {useCallback, useMemo, useState} from 'react';
import {flushSync} from 'react-dom';
import {useNavigate, useSearchParams} from 'react-router-dom';

import AutomationWorkflowConfigurationsEmptyState from './AutomationWorkflowConfigurationsEmptyState';
import AutomationWorkflowConfigurationsFilterTitle from './AutomationWorkflowConfigurationsFilterTitle';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];

interface ConfigurationDialogStateI {
    configuration?: AutomationWorkflowProjectConfigurationType;
    project?: AutomationWorkflowProjectType;
    updateProjectVersion?: boolean;
}

const AutomationWorkflowConfigurations = () => {
    const [configurationDialog, setConfigurationDialog] = useState<ConfigurationDialogStateI | undefined>();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setCurrentEnvironmentId = useEnvironmentStore((state) => state.setCurrentEnvironmentId);

    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const {openAutomationWorkflow} = useOpenInEmbeddedEditor();

    const {
        data: projectsData,
        error: projectsError,
        isLoading: projectsIsLoading,
    } = useAutomationWorkflowProjectsQuery({environmentId: String(currentEnvironmentId)});

    const initialLoadComplete = useInitialLoadComplete(projectsIsLoading);

    const projectId = searchParams.get('projectId');

    const publishedProjects = useMemo(
        () => (projectsData?.automationWorkflowProjects ?? []).filter((project) => project.published),
        [projectsData]
    );

    const selectedProject = projectId ? publishedProjects.find((project) => project.id === projectId) : undefined;

    const configuredProjects = useMemo(
        () => publishedProjects.filter((project) => project.configurationCount > 0),
        [publishedProjects]
    );

    const listedProjects = selectedProject
        ? configuredProjects.filter((project) => project.id === selectedProject.id)
        : configuredProjects;

    const isFirstRun = !projectsIsLoading && !projectId && configuredProjects.length === 0;

    const newConfigurationButton = (
        <Button label="New Project Configuration" onClick={() => setConfigurationDialog({project: selectedProject})} />
    );

    const handleCreateConfiguration = useCallback(() => setConfigurationDialog({}), []);

    const handleGoToAutomations = useCallback(() => {
        flushSync(() => setCurrentEnvironmentId(DEVELOPMENT_ENVIRONMENT));

        void navigate('/embedded/automation-workflows');
    }, [navigate, setCurrentEnvironmentId]);

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={listedProjects.length > 0 && newConfigurationButton}
                    title={
                        isFirstRun ? (
                            'Configurations'
                        ) : (
                            <AutomationWorkflowConfigurationsFilterTitle projectName={selectedProject?.name} />
                        )
                    }
                />
            }
            leftSidebarBody={
                <LeftSidebarFilterNav
                    items={publishedProjects.map((project) => ({
                        current: project.id === projectId,
                        id: project.id,
                        name: project.name,
                        toLink: `?projectId=${project.id}`,
                    }))}
                    leadItem={{current: !projectId, name: 'All Projects'}}
                    loading={projectsIsLoading}
                    title="Projects"
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="Configurations" />}
            leftSidebarHidden={!initialLoadComplete || isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[projectsError]} loading={projectsIsLoading}>
                {listedProjects.length > 0 ? (
                    <div className="flex w-full flex-col gap-2 self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                        {listedProjects.map((project) => (
                            <section key={project.id}>
                                <ListGroupHeader
                                    action={
                                        project.workflowTemplates.length > 0 && (
                                            <OpenInButton
                                                label="Open project"
                                                onClick={() =>
                                                    openAutomationWorkflow(project.workflowTemplates[0]?.workflowUuid)
                                                }
                                            />
                                        )
                                    }
                                    icon={<FolderIcon aria-hidden="true" className="size-6 shrink-0" />}
                                    name={project.name}
                                />

                                <AutomationWorkflowProjectConfigurationList
                                    environmentId={currentEnvironmentId}
                                    onCreate={() => setConfigurationDialog({project})}
                                    onEdit={(configuration) => setConfigurationDialog({configuration, project})}
                                    onUpdateVersion={(configuration) =>
                                        setConfigurationDialog({configuration, project, updateProjectVersion: true})
                                    }
                                    project={project}
                                    showOpenInProject
                                />
                            </section>
                        ))}
                    </div>
                ) : selectedProject ? (
                    <EmptyList
                        button={newConfigurationButton}
                        icon={<SlidersHorizontalIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message={`${selectedProject.name} has no configurations in this environment.`}
                        title="No Matching Configurations"
                    />
                ) : projectId ? (
                    <EmptyList
                        icon={<SlidersHorizontalIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="The selected project is not published."
                        title="No Matching Project"
                    />
                ) : (
                    <AutomationWorkflowConfigurationsEmptyState
                        hasProject={(projectsData?.automationWorkflowProjects ?? []).length > 0}
                        hasPublishedProject={publishedProjects.length > 0}
                        onCreateConfiguration={handleCreateConfiguration}
                        onGoToAutomations={handleGoToAutomations}
                    />
                )}
            </PageLoader>

            {configurationDialog && (
                <AutomationWorkflowProjectConfigurationDialog
                    configuration={configurationDialog.configuration}
                    onClose={() => setConfigurationDialog(undefined)}
                    project={configurationDialog.project}
                    projects={publishedProjects}
                    updateProjectVersion={configurationDialog.updateProjectVersion}
                />
            )}

            <AutomationWorkflowExecutionsSheet />
        </LayoutContainer>
    );
};

export default AutomationWorkflowConfigurations;
