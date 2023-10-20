import SidebarContentLayout from '../../components/Layouts/SidebarContentLayout';
import IntegrationList from './IntegrationList';
import IntegrationModal from './IntegrationModal';
import React from 'react';
import LeftSidebar from './LeftSidebar';
import PageHeader from '../../components/PageHeader/PageHeader';

const Integrations: React.FC = () => (
    <SidebarContentLayout
        header={
            <PageHeader right={<IntegrationModal />} title="All Integrations" />
        }
        leftSidebarHeader={
            <PageHeader leftSidebar={true} title="Integrations" />
        }
        leftSidebarBody={<LeftSidebar />}
    >
        <IntegrationList />
    </SidebarContentLayout>
);

export default Integrations;
