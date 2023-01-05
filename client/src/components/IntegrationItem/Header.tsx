import React from 'react';
import {Dropdown} from './Dropdown';
import {Name} from './Name';
import {Status} from './Status';

const Header: React.FC<{
    id: string;
    name: string;
    status: boolean;
}> = ({id, name, status}) => {
    return (
        <>
            <div className="flex flex-row">
                <div className="w-24">
                    <Name name={name} />
                </div>
                <div className="w-96">
                    <Status status={status} />
                </div>
                <div className="w-96">
                    <Dropdown id={id} />
                </div>
            </div>
        </>
    );
};

export default Header;
