/* eslint-disable tailwindcss/no-custom-classname */

import Modal from 'components/Modal/Modal';

import {SidebarContentLayout} from '../../components/Layouts/SidebarContentLayout';
import {IntegrationList} from './IntegrationList';
import React from 'react';
import {Input} from '../../components/Input/Input';
import {TextArea} from '../../components/TextArea/TextArea';
import {MultiSelect} from '../../components/MultiSelect/MultiSelect';

const options = [
    {value: 'chocolate', label: 'Chocolate'},
    {value: 'strawberry', label: 'Strawberry'},
    {value: 'vanilla', label: 'Vanilla'},
];

const IntegrationModal: React.FC = () => (
    <Modal
        confirmButtonLabel="Create"
        description="Use this to create your integration which will contain related workflows"
        triggerLabel="Create Integration"
        title="Create Integration"
    >
        <Input label="Name" name="name" placeholder="My CRM Integration" />

        <TextArea
            label="Description"
            name="description"
            placeholder="Cute description of your integration"
        />

        <Input
            label="Category"
            name="category"
            placeholder="Marketing, Sales, Social Media..."
        />

        <MultiSelect label="Tags" name="tags" options={options} />
    </Modal>
);

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
