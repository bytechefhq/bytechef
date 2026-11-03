import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import AutomationWorkflowExecutionsSheet from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-executions-sheet/AutomationWorkflowExecutionsSheet';
import AutomationWorkflowProjectConfigurationDialog from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-configuration-dialog/AutomationWorkflowProjectConfigurationDialog';
import AutomationWorkflowProjectConfigurationList from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-configuration-list/AutomationWorkflowProjectConfigurationList';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentScopeNote} from '@/shared/hooks/useEnvironmentScopeNote';
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
import {ChevronDownIcon, Settings2Icon} from 'lucide-react';
import {useMemo, useState} from 'react';
import {flushSync} from 'react-dom';
import {useNavigate, useSearchParams} from 'react-router-dom';

import AutomationWorkflowConfigurationsFilterTitle from './AutomationWorkflowConfigurationsFilterTitle';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type AutomationWorkflowProjectConfigurationType =
    AutomationWorkflowProjectConfigurationsQuery['automationWorkflowProjectConfigurations'][number];

interface ConfigurationDialogStateI {
    configuration?: AutomationWorkflowProjectConfigurationType;
    project: AutomationWorkflowProjectType;
    updateProjectVersion?: boolean;
}

const AutomationWorkflowConfigurations = () => {
    const [configurationDialog, setConfigurationDialog] = useState<ConfigurationDialogStateI | undefined>();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setCurrentEnvironmentId = useEnvironmentStore((state) => state.setCurrentEnvironmentId);

    const environmentScopeNote = useEnvironmentScopeNote('Configurations');

    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

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

    const listedProjects = selectedProject ? [selectedProject] : configuredProjects;

    const isFirstRun = !projectsIsLoading && publishedProjects.length === 0;

    const newConfigurationButton = selectedProject ? (
        <Button label="New Configuration" onClick={() => setConfigurationDialog({project: selectedProject})} />
    ) : (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button aria-label="New Configuration">
                    New Configuration
                    <ChevronDownIcon />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                {publishedProjects.map((project) => (
                    <DropdownMenuItem key={project.id} onClick={() => setConfigurationDialog({project})}>
                        {project.name}
                    </DropdownMenuItem>
                ))}
            </DropdownMenuContent>
        </DropdownMenu>
    );

    const handleGoToAutomations = () => {
        flushSync(() => setCurrentEnvironmentId(DEVELOPMENT_ENVIRONMENT));

        void navigate('/embedded/automation-workflows');
    };

    return (
        <LayoutContainer
            header={
                !isFirstRun && (
                    <Header
                        centerTitle={true}
                        position="main"
                        right={newConfigurationButton}
                        title={<AutomationWorkflowConfigurationsFilterTitle projectName={selectedProject?.name} />}
                    />
                )
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
                    <div className="w-full divide-y divide-border/50 self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
                        {listedProjects.map((project) => (
                            <section aria-label={project.name} className="py-3" key={project.id}>
                                <h2 className="mb-2 text-base font-semibold">{project.name}</h2>

                                <AutomationWorkflowProjectConfigurationList
                                    environmentId={currentEnvironmentId}
                                    onCreate={() => setConfigurationDialog({project})}
                                    onEdit={(configuration) => setConfigurationDialog({configuration, project})}
                                    onUpdateVersion={(configuration) =>
                                        setConfigurationDialog({configuration, project, updateProjectVersion: true})
                                    }
                                    project={project}
                                />
                            </section>
                        ))}
                    </div>
                ) : projectId ? (
                    <EmptyList
                        icon={<Settings2Icon className="size-24 text-stroke-neutral-tertiary" />}
                        message="The selected project is not published."
                        title="No Matching Project"
                    />
                ) : publishedProjects.length > 0 ? (
                    <EmptyState
                        actions={newConfigurationButton}
                        description="A configuration runs a published version of an automation project for connected users in this environment."
                        icon={<Settings2Icon />}
                        note={environmentScopeNote}
                        title="No configurations yet"
                    />
                ) : (
                    <EmptyState
                        actions={<Button label="Go to Automations" onClick={handleGoToAutomations} />}
                        description="A configuration runs a published version of an automation project in this environment. Build and publish a project in the Development environment first."
                        icon={<Settings2Icon />}
                        note={environmentScopeNote}
                        title="Publish a project to configure it"
                    />
                )}
            </PageLoader>

            {configurationDialog && (
                <AutomationWorkflowProjectConfigurationDialog
                    configuration={configurationDialog.configuration}
                    onClose={() => setConfigurationDialog(undefined)}
                    project={configurationDialog.project}
                    updateProjectVersion={configurationDialog.updateProjectVersion}
                />
            )}

            <AutomationWorkflowExecutionsSheet />
        </LayoutContainer>
    );
};

export default AutomationWorkflowConfigurations;
