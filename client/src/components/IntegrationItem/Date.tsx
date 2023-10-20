import {CalendarIcon} from '@radix-ui/react-icons';
import React from 'react';

export const Date: React.FC<{date?: Date}> = ({date}) => {
    return (
        <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
            <CalendarIcon
                className="mr-1.5 h-5 w-5 text-gray-400"
                aria-hidden="true"
            />
            Last modified {date && date.toLocaleDateString()}
        </div>
    );
};
