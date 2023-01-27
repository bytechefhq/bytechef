import React from 'react';
import {Dropdown} from './Dropdown';
import {Name} from './Name';
import {Status} from './Status';

const Header: React.FC<{
    name: string;
    status: boolean;
    description?: string;
    id?: number;
}> = ({id, name, status, description = 'Description not available'}) => {
    return (
        <div className="mb-3 flex items-center justify-between">
            <div className="">
                <Name name={name} description={description} />

                <Status status={status} />
            </div>

            <Dropdown id={id} />
        </div>
    );
};

export default Header;
