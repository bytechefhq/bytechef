import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import IntegrationLeftSidebarNav from '@/pages/embedded/integration/components/IntegrationLeftSidebarNav';

const IntegrationUserMetadata = () => {
    return (
        <LayoutContainer
            header={<PageHeader position="main" title="User Metadata" />}
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Pipedrive Integration" />
            }
            leftSidebarBody={<IntegrationLeftSidebarNav />}
        >
            <div>Settings</div>
        </LayoutContainer>
    );
};

export default IntegrationUserMetadata;
