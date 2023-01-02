import React from 'react';
import {Dropdown} from './Dropdown';
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
            <Dropdown />
        </div>
    );
};

export default Header;
