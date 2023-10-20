import IntegrationLeftSidebarNav from '@/ee/pages/embedded/integration/components/IntegrationLeftSidebarNav';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';

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
