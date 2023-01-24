import React from 'react';
import {Cross1Icon} from '@radix-ui/react-icons';
import {TagModel} from '../../data-access/integration';

export const Tag: React.FC<{tag: TagModel}> = ({tag}) => {
    return (
        <div className="ml-4 inline-flex items-center rounded-full border bg-gray-200 px-3 py-1 text-xs font-bold text-gray-700">
            {tag.name}

            <Cross1Icon />
        </div>
    );
};
