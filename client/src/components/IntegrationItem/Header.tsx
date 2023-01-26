import React from 'react';
import {Description} from './Description';
import {Dropdown} from './Dropdown';
import {Name} from './Name';
import {Status} from './Status';

const Header: React.FC<{
    id?: number;
    name: string;
    status: boolean;
    description?: string;
}> = ({id, name, status, description}) => {
    return (
        <div className="flex table-auto justify-between">
            <div className="list-inside">
                <Name name={name} />

                <Status status={status} />

                {description && <Description description={description} />}
            </div>

            <div className="mx-4">
                <Dropdown id={id} />
            </div>
        </div>
    );
};

export default Header;
