/* eslint-disable tailwindcss/no-custom-classname */

import {SidebarContentLayout} from '../../components/Layouts/SidebarContentLayout';
import {IntegrationList} from './IntegrationList';
import IntegrationModal from './IntegrationModal';
import React from 'react';

const Integrations: React.FC = () => (
    <SidebarContentLayout
        headerProps={{
            buttonLabel: 'New Integration',
            right: <IntegrationModal />,
            subTitle: 'All Integrations',
        }}
        title="Integrations"
    >
        <IntegrationList />
    </SidebarContentLayout>
);

export default Integrations;
