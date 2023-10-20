import {SidebarContentLayout} from '../../components/Layouts/SidebarContentLayout';
import {IntegrationList} from './IntegrationList';
import React from 'react';
import Button from '../../components/Button/Button';

const Integrations: React.FC = () => (
    <SidebarContentLayout
        title={'Integrations'}
        subTitle="All Integrations"
        topRight={<Button label="New Integration" />}
    >
        <IntegrationList />
    </SidebarContentLayout>
);

export default Integrations;
