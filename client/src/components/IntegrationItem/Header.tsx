import React from 'react';
import {DropdownTrigger} from './DropdownTrigger';
import {Name} from './Name';
import {Status} from './Status';

const Header: React.FC<{
    name: string;
    status: string;
    dropdownTrigger: string;
}> = ({name, status, dropdownTrigger}) => {
    return (
        <div>
            <Name name={name} />

            <Status status={status} />

            <DropdownTrigger dropdownTrigger={dropdownTrigger} />
        </div>
    );
};

export default Header;
