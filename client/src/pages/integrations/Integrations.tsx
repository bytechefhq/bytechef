import PageHeader from 'components/PageHeader/PageHeader';
import {SidebarContentLayout} from '../../components/Layouts/SidebarContentLayout';
import {IntegrationList} from './IntegrationList';
import React from 'react';

const Integrations: React.FC = () => (
    <SidebarContentLayout title={'Integrations'}>
        <PageHeader subTitle="All Integrations" buttonLabel="New Integration" />

        <IntegrationList />
    </SidebarContentLayout>
);

export default Integrations;
