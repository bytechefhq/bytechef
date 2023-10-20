import {CalendarIcon} from '@radix-ui/react-icons';
import React from 'react';

export const Date: React.FC<{date: Date}> = ({date}) => {
    return (
        <span className="mt-2 flex items-center text-xs text-gray-500 sm:mt-0">
            <CalendarIcon
                className="mr-1 h-4 w-4 text-gray-400"
                aria-hidden="true"
            />
            Last modified: {date.toLocaleDateString()}
        </span>
    );
};
