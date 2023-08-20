import IntegrationLeftSidebarNav from '@/ee/pages/embedded/integration/components/IntegrationLeftSidebarNav';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';

const IntegrationWorkflowEditor = () => {
    return (
        <LayoutContainer
            header={<PageHeader position="main" title="Workflow Editor" />}
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Pipedrive Integration" />
            }
            leftSidebarBody={<IntegrationLeftSidebarNav />}
        >
            <div>Workflow Editor</div>
        </LayoutContainer>
    );
};

export default IntegrationWorkflowEditor;
