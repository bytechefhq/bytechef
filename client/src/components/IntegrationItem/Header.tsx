import React from 'react';
import {Dropdown} from './Dropdown';
import {Name} from './Name';
import {Status} from './Status';

const Header: React.FC<{
    id?: number;
    name: string;
    status: boolean;
}> = ({id, name, status}) => {
    return (
        <div className="grid grid-cols-3 gap-4">
            <span className="... col-span-2 flex flex-row space-x-4 ">
                <Name name={name} />

                <Status status={status} />
            </span>

            <div className="... grid justify-items-end">
                <Dropdown id={id} />
            </div>
        </div>
    );
};

export default Header;
