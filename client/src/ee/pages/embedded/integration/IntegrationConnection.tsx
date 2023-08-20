import IntegrationLeftSidebarNav from '@/ee/pages/embedded/integration/components/IntegrationLeftSidebarNav';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';

const IntegrationConnection = () => {
    return (
        <LayoutContainer
            header={<PageHeader position="main" title="Connection" />}
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Pipedrive Integration" />
            }
            leftSidebarBody={<IntegrationLeftSidebarNav />}
        >
            <div>Connection</div>
        </LayoutContainer>
    );
};

export default IntegrationConnection;
