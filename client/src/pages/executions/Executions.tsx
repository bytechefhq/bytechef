import LayoutContainer from '../../layouts/LayoutContainer/LayoutContainer';
import PageHeader from '../../components/PageHeader/PageHeader';
import React from 'react';

export const Executions = () => {
    return (
        <LayoutContainer
            header={<PageHeader title="All Executions" />}
            leftSidebarHeader={<PageHeader leftSidebar title="Executions" />}
        ></LayoutContainer>
    );
};

export default Executions;
