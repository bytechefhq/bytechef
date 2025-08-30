import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import ConnectedUserFilterTitle from '@/ee/pages/embedded/automation-workflows/components/ConnectedUserFilterTitle';
import EnvironmentLeftSidebarNav from '@/ee/pages/embedded/automation-workflows/components/EnvironmentLeftSidebarNav';
import ConnectedUserProjectList from '@/ee/pages/embedded/automation-workflows/components/connected-user-project-list/ConnectedUserProjectList';
import {ConnectedUser, ConnectedUserFromJSON} from '@/ee/shared/middleware/embedded/connected-user';
import {useGetConnectedUsersQuery} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {EnvironmentEnum, InputMaybe, useConnectedUserProjectsQuery} from '@/shared/middleware/graphql';
import {Workflow} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

import ConnectedUsersLeftSidebarNav from './components/ConnectedUsersLeftSidebarNav';

function getEnvironment(environment: number | undefined): EnvironmentEnum | undefined {
    return environment === undefined
        ? undefined
        : environment === 1
          ? EnvironmentEnum.Development
          : environment === 2
            ? EnvironmentEnum.Staging
            : EnvironmentEnum.Production;
}

const AutomationWorkflows = () => {
    const [searchParams] = useSearchParams();

    const connectedUserId = searchParams.get('connectedUserId')
        ? parseInt(searchParams.get('connectedUserId')!)
        : undefined;
    const environment = getEnvironment(
        searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined
    );

    const {data: connectedUsersPage, isLoading: isConnectedUsersPageLoading} = useGetConnectedUsersQuery({
        pageNumber: 0,
    });

    const {data, isLoading: isConnectedUserProjectsLoading} = useConnectedUserProjectsQuery({
        connectedUserId: connectedUserId?.toString(),
        environment: environment as InputMaybe<EnvironmentEnum> | undefined,
    });

    const connectedUsers =
        connectedUsersPage?.content?.map((connectedUser: object) => ConnectedUserFromJSON(connectedUser)) || [];

    return (
        <LayoutContainer
            header={
                connectedUsers &&
                connectedUsers?.length > 0 && (
                    <Header
                        centerTitle={true}
                        position="main"
                        title={
                            <ConnectedUserFilterTitle
                                connectedUsers={connectedUsers as ConnectedUser[]}
                                environment={environment}
                                filterData={{id: connectedUserId}}
                            />
                        }
                    />
                )
            }
            leftSidebarBody={
                <>
                    <EnvironmentLeftSidebarNav connectedUserId={connectedUserId} environment={environment} />

                    <ConnectedUsersLeftSidebarNav
                        connectedUserId={connectedUserId}
                        connectedUsers={connectedUsers}
                        environment={environment}
                    />
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Automation Workflows" />}
            leftSidebarWidth="64"
        >
            <PageLoader loading={isConnectedUsersPageLoading || isConnectedUserProjectsLoading}>
                {data?.connectedUserProjects && data?.connectedUserProjects?.length > 0 ? (
                    <div className="w-full divide-y divide-border/50 px-4 2xl:mx-auto 2xl:w-4/5">
                        <ConnectedUserProjectList connectedUserProjects={data.connectedUserProjects} />
                    </div>
                ) : (
                    <EmptyList
                        icon={<Workflow className="size-24 text-gray-300" />}
                        message="There is no yet created automation workflows from the Connected Users."
                        title="No Automation Workflows"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default AutomationWorkflows;
