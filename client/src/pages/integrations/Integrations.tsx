import SidebarContentLayout from '../../components/Layouts/SidebarContentLayout';
import IntegrationList from './IntegrationList';
import IntegrationModal from './IntegrationModal';
import React from 'react';
import LeftSidebar from './LeftSidebar';
import PageHeader from '../../components/PageHeader/PageHeader';

const Integrations = () => (
    <SidebarContentLayout
        header={
            <PageHeader
                position={'main'}
                right={<IntegrationModal />}
                title="All Integrations"
            />
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
