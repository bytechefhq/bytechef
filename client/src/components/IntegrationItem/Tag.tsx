import React from 'react';
import {Cross1Icon} from '@radix-ui/react-icons';

export const Tag: React.FC<{tag?: string}> = ({tag}) => {
    return (
        <div className="ml-4 inline-flex items-center rounded-full border bg-gray-200 px-3 py-1 text-xs font-bold text-gray-700">
            {tag}

            <Cross1Icon />
        </div>
    );
};
