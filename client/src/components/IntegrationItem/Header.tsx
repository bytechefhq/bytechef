import React from 'react';
import {DropdownTrigger} from './DropdownTrigger';
import {Name} from './Name';
import {Status} from './Status';

const Header: React.FC<{
    name: string;
    status: boolean;
    dropdownTrigger: string;
}> = ({name, status, dropdownTrigger}) => {
    return (
        <>
            <div className="... flex flex-row">
                <div className="w-24">
                    <Name name={name} />
                </div>
                <div className="w-96">
                    <Status status={status} />
                </div>
                <div className="w-96">
                    <DropdownTrigger dropdownTrigger={dropdownTrigger} />
                </div>
            </div>
        </>
    );
};

export default Header;
