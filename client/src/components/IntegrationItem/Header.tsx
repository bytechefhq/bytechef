import React from 'react';
import {Dropdown} from './Dropdown';
import {Name} from './Name';
import {Status} from './Status';

const Header: React.FC<{
    id: string;
    name: string;
    status: string;
    dropdownTrigger: string;
}> = ({id, name, status, dropdownTrigger}) => {
    return (
        <div>
            <Name name={name} />

            <Status status={status} />
            <Dropdown id={id} />
        </div>
    );
};

export default Header;
