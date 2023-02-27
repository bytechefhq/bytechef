import LayoutContainer from '../../layouts/LayoutContainer/LayoutContainer';
import PageHeader from '../../components/PageHeader/PageHeader';
import React from 'react';

export const Instances = () => {
    return (
        <LayoutContainer
            header={<PageHeader title="All Instances" />}
            leftSidebarHeader={<PageHeader leftSidebar title="Instances" />}
        ></LayoutContainer>
    );
};

export default Instances;
