/* eslint-disable tailwindcss/no-custom-classname */

import PageHeader from 'components/PageHeader/PageHeader';
import Modal from 'components/Modal/Modal';

import {SidebarContentLayout} from '../../components/Layouts/SidebarContentLayout';
import {IntegrationList} from './IntegrationList';
import React from 'react';
import Select from 'react-select';

const options = [
    {value: 'chocolate', label: 'Chocolate'},
    {value: 'strawberry', label: 'Strawberry'},
    {value: 'vanilla', label: 'Vanilla'},
];

const Integrations: React.FC = () => (
    <SidebarContentLayout title={'Integrations'} subTitle={''}>
        <PageHeader
            title="All Integrations"
            buttonLabel="New Integration"
            right={
                <Modal
                    cancelButtonLabel="Cancel"
                    confirmButtonLabel="Create"
                    description="Use this to create your integration which will contain related workflows"
                    triggerLabel="Create Integration"
                    title="Create Integration"
                >
                    <fieldset className="Fieldset">
                        <label className="Label" htmlFor="name">
                            Name
                        </label>

                        <input
                            className="Input"
                            id="name"
                            placeholder="John Doe"
                        />
                    </fieldset>

                    <fieldset className="Fieldset">
                        <label className="Label" htmlFor="username">
                            Description
                        </label>

                        <textarea
                            className="Input"
                            id="username"
                            placeholder="Cute description of your integration"
                        />
                    </fieldset>

                    <fieldset className="Fieldset">
                        <label htmlFor="category" className="Label">
                            Category
                        </label>

                        <input
                            className="Input"
                            id="category"
                            placeholder="Marketing, Sales, Social Media..."
                        />
                    </fieldset>

                    <fieldset className="Fieldset">
                        <label htmlFor="category" className="Label">
                            Tags
                        </label>

                        <Select options={options} isMulti={true} />
                    </fieldset>
                </Modal>
            }
        />

        <IntegrationList />
    </SidebarContentLayout>
);

export default Integrations;
