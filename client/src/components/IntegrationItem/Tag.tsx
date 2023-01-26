import React from 'react';
import {Cross1Icon} from '@radix-ui/react-icons';
import {TagModel} from '../../data-access/integration';

export const Tag: React.FC<{tag: TagModel}> = ({tag}) => {
    return (
        <div className=" inline-flex items-center rounded-full border bg-gray-200 px-2 py-1 text-xs font-bold text-gray-700">
            {tag.name}
            <div className="px-2">
                <Cross1Icon />
            </div>
        </div>
    );
};
