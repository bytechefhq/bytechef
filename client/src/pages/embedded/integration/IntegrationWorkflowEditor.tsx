import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import IntegrationLeftSidebarNav from '@/pages/embedded/integration/components/IntegrationLeftSidebarNav';

const IntegrationWorkflowEditor = () => {
    return (
        <LayoutContainer
            header={<PageHeader position="main" title="Workflow Editor" />}
            leftSidebarHeader={
                <PageHeader position="sidebar" title="Workflow Editor" />
            }
            leftSidebarBody={<IntegrationLeftSidebarNav />}
        >
            <div>Workflow Editor</div>
        </LayoutContainer>
    );
};

export default IntegrationWorkflowEditor;
