import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import IntegrationLeftSidebarNav from '@/pages/embedded/integration/components/IntegrationLeftSidebarNav';

const IntegrationSettings = () => {
    return (
        <LayoutContainer
            header={<PageHeader position="main" title="Settings" />}
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Settings" />
            }
            leftSidebarBody={<IntegrationLeftSidebarNav />}
        >
            <div>Settings</div>
        </LayoutContainer>
    );
};

export default IntegrationSettings;
