import React from 'react';

import PageHeader from '../../../components/PageHeader/PageHeader';
import LayoutContainer from '../../../layouts/LayoutContainer/LayoutContainer';

export const Instances = () => {
    return (
        <LayoutContainer
            header={<PageHeader position="main" title="All Instances" />}
            leftSidebarHeader={<PageHeader leftSidebar title="Instances" />}
        ></LayoutContainer>
    );
};

export default Instances;
