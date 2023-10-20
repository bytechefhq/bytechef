import React from 'react';
import {Name} from './Name';
import {Status} from './Status';
import {Dropdown, DropDownMenuItem} from '../DropDown/Dropdown';

const menuItems: DropDownMenuItem[] = [
    {
        label: 'Edit',
    },
    {
        label: 'Enable',
    },
    {
        label: 'Duplicate',
    },
    {
        label: 'New Workflow',
    },
    {
        separator: true,
    },
    {
        label: 'Delete',
    },
];

const Header: React.FC<{
    name: string;
    status: boolean;
    description?: string;
    id?: number;
}> = ({id, name, status, description = 'Description not available'}) => {
    return (
        <div className="relative mb-3 flex items-center justify-between">
            <div className="">
                <Status status={status} />

                <Name name={name} description={description} />
            </div>

            <Dropdown id={id} menuItems={menuItems} />
        </div>
    );
};

export default Header;
