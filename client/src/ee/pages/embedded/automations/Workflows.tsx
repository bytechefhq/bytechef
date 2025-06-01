import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {Settings2Icon} from 'lucide-react';
import {useSearchParams} from 'react-router-dom';

const Workflows = () => {
    const [searchParams] = useSearchParams();

    const environment = searchParams.get('environment') ? parseInt(searchParams.get('environment')!) : undefined;

    return (
        <LayoutContainer
            header={<Header centerTitle={true} position="main" title="" />}
            leftSidebarBody={
                <>
                    <LeftSidebarNav
                        body={
                            <>
                                {[
                                    {label: 'All Environments'},
                                    {label: 'Development', value: 1},
                                    {label: 'Staging', value: 2},
                                    {label: 'Production', value: 3},
                                ]?.map((item) => (
                                    <LeftSidebarNavItem
                                        item={{
                                            current: environment === item.value,
                                            id: item.value,
                                            name: item.label,
                                        }}
                                        key={item.value ?? ''}
                                        toLink={`?environment=${item.value ?? ''}`}
                                    />
                                ))}
                            </>
                        }
                        title="Environments"
                    />
                </>
            }
            leftSidebarHeader={<Header position="sidebar" title="Automation Workflows" />}
            leftSidebarWidth="64"
        >
            <PageLoader errors={[]} loading={false}>
                <EmptyList
                    icon={<Settings2Icon className="size-24 text-gray-300" />}
                    message="Get started by embedding the Workflow Builder."
                    title="No Automation Workflows"
                />
            </PageLoader>
        </LayoutContainer>
    );
};

export default Workflows;
