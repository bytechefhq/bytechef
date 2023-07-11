import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import IntegrationLeftSidebarNav from '@/pages/embedded/integration/components/IntegrationLeftSidebarNav';

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
