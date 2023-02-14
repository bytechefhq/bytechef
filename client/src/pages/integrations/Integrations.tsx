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
                className="flex place-self-center sm:w-full 2xl:w-4/5"
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
