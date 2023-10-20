import SidebarContentLayout from '../../components/Layouts/SidebarContentLayout';
import IntegrationList from './IntegrationList';
import IntegrationModal from './IntegrationModal';
import React from 'react';
import IntegrationsSidebar from './IntegrationsSidebar';

const Integrations: React.FC = () => (
    <SidebarContentLayout
        headerProps={{
            buttonLabel: 'New Integration',
            right: <IntegrationModal />,
            subTitle: 'All Integrations',
        }}
        sidebar={<IntegrationsSidebar />}
        title="Integrations"
    >
        <IntegrationList />
    </SidebarContentLayout>
);

export default Integrations;
