import Button from '@/components/Button/Button';
import EmptyFilterResult from '@/components/EmptyFilterResult';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {A2aServer} from '@/shared/middleware/graphql';
import {NetworkIcon} from 'lucide-react';

import A2aServerDialog from './components/A2aServerDialog';
import A2aServerList from './components/A2aServerList';
import A2aServersFilterTitle from './components/A2aServersFilterTitle';
import A2aServersLeftSidebarNav from './components/A2aServersLeftSidebarNav';
import useA2aServers from './hooks/useA2aServers';

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
                            ''
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
            leftSidebarWidth="64"
        >
            <PageLoader errors={[a2aServersError, tagsError]} loading={a2aServersIsLoading || tagsIsLoading}>
                {filteredA2aServers.length > 0 ? (
                    <A2aServerList a2aServers={filteredA2aServers as A2aServer[]} tags={tags} />
                ) : a2aServers.length > 0 ? (
                    <EmptyFilterResult entityName="A2A servers" entityTitle="A2A Servers" />
                ) : (
                    <EmptyList
                        button={<A2aServerDialog triggerNode={<Button label="Create A2A Server" />} />}
                        icon={<NetworkIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message="Get started by creating a new A2A server to expose an agent-backed workflow."
                        title="No A2A Servers"
                    />
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default A2aServers;
