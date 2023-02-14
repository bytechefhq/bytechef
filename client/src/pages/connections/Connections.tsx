import SidebarContentLayout from '../../components/Layouts/SidebarContentLayout';
import PageHeader from '../../components/PageHeader/PageHeader';
import React from 'react';

const Connections = () => {
    return (
        <SidebarContentLayout
            header={<PageHeader title="All Connections" />}
            leftSidebarHeader={
                <PageHeader leftSidebar={true} title="Connections" />
            }
        />
    );
};

export default Connections;
