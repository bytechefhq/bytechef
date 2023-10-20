import React from 'react';
import {Dropdown} from './Dropdown';
import {Name} from './Name';
import {Status} from './Status';

const Header: React.FC<{
    name: string;
    status: boolean;
    description?: string;
    id?: number;
}> = ({id, name, status, description}) => {
    return (
        <div className="flex justify-between">
            <div className="">
                <Name
                    name={name}
                    description={description || 'Description not available'}
                />

                <Status status={status} />
            </div>

            <div className="mx-4">
                <Dropdown id={id} />
            </div>
        </div>
    );
};

export default Header;
