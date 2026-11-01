import Button from '@/components/Button/Button';
import EmptyFilterResult from '@/components/EmptyFilterResult';
import EmptyState from '@/components/EmptyState/EmptyState';
import JourneySteps from '@/components/JourneySteps/JourneySteps';
import PageLoader from '@/components/PageLoader';
import {A2A_SERVERS_DOCUMENTATION_URL} from '@/pages/automation/journey/automationJourneySteps';
import ProjectDeploymentWorkflowExecutionsSheet from '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useInitialLoadComplete} from '@/shared/hooks/useInitialLoadComplete';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {A2aServer} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {NetworkIcon} from 'lucide-react';

import A2aServerDialog from './components/A2aServerDialog';
import A2aServerList from './components/A2aServerList';
import A2aServersFilterTitle from './components/A2aServersFilterTitle';
import A2aServersLeftSidebarNav from './components/A2aServersLeftSidebarNav';
import useA2aServers from './hooks/useA2aServers';

const A2A_SERVER_STEPS = [
    {
        id: 'createA2aServer',
        label: 'Create an A2A server',
        status: 'current' as const,
    },
    {id: 'addSkills', label: 'Add workflows as skills', status: 'upcoming' as const},
    {id: 'connectAgent', label: 'Connect another agent', status: 'upcoming' as const},
];

const A2aServers = () => {
    const {
        a2aProjectsIsLoading,
        a2aServers,
        a2aServersError,
        a2aServersIsLoading,
        filterData,
        filteredA2aServers,
        tags,
        tagsError,
        tagsIsLoading,
        uniqueProjects,
    } = useA2aServers();

    const initialLoadComplete = useInitialLoadComplete(a2aServersIsLoading);

    const isFirstRun = !a2aServersIsLoading && !tagsIsLoading && !a2aServersError && a2aServers.length === 0;

    let pageContent;

    if (filteredA2aServers.length > 0) {
        pageContent = (
            <WorkflowReadOnlyProvider value={{useGetComponentDefinitionsQuery}}>
                <A2aServerList a2aServers={filteredA2aServers as A2aServer[]} tags={tags} />

                <ProjectDeploymentWorkflowExecutionsSheet />
            </WorkflowReadOnlyProvider>
        );
    } else if (a2aServers.length > 0) {
        pageContent = <EmptyFilterResult entityName="A2A servers" entityTitle="A2A Servers" />;
    } else {
        pageContent = (
            <EmptyState
                actions={<A2aServerDialog triggerNode={<Button label="Create A2A Server" />} />}
                description="An A2A server publishes an agent that other agents can discover and hand tasks to, over the Agent2Agent protocol. Each workflow you add becomes one of its skills."
                docsLink={{href: A2A_SERVERS_DOCUMENTATION_URL, label: 'How A2A servers work'}}
                icon={<NetworkIcon />}
                title="Create your first A2A server"
            >
                <JourneySteps aria-label="Getting started" steps={A2A_SERVER_STEPS} />
            </EmptyState>
        );
    }

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        <div className="flex items-center gap-1">
                            {a2aServers.length > 0 && (
                                <A2aServerDialog triggerNode={<Button label="New A2A Server" />} />
                            )}
                        </div>
                    }
                    title={
                        a2aServers.length > 0 ? (
                            <A2aServersFilterTitle
                                filterData={filterData}
                                tags={tags}
                                uniqueProjects={uniqueProjects}
                            />
                        ) : (
                            isFirstRun && 'A2A Servers'
                        )
                    }
                />
            }
            leftSidebarBody={
                <A2aServersLeftSidebarNav
                    a2aProjectsIsLoading={a2aProjectsIsLoading}
                    filterData={filterData}
                    tags={tags}
                    tagsIsLoading={tagsIsLoading}
                    uniqueProjects={uniqueProjects}
                />
            }
            leftSidebarHeader={<Header position="sidebar" title="A2A Servers" />}
            leftSidebarHidden={!initialLoadComplete || isFirstRun}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[a2aServersError, tagsError]} loading={a2aServersIsLoading || tagsIsLoading}>
                {pageContent}
            </PageLoader>
        </LayoutContainer>
    );
};

export default A2aServers;
