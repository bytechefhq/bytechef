import React from 'react';

export const Status: React.FC<{status: boolean}> = ({status}) => {
    let color = status ? 'green' : 'red';
    let label = status ? 'Enabled' : 'Disabled';

    return (
        <span
            className={`mr-2 rounded bg-${color}-100 px-2.5 py-0.5 text-sm font-medium text-${color}-800 dark:bg-${color}-200 dark:text-${color}-900`}
        >
            {label}
        </span>
    );
};
