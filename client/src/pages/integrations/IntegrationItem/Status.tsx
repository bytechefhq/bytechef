import React from 'react';
import cx from 'classnames';

export const Status: React.FC<{status: boolean}> = ({status}) => {
    const label = status ? 'Enabled' : 'Disabled';

    return (
        <span
            className={cx(
                'mr-2 rounded px-2.5 py-0.5 text-sm font-medium',
                status ? 'bg-green-100' : 'bg-red-100',
                status ? 'text-green-800' : 'text-red-800',
                status ? 'dark:bg-green-200' : 'dark:bg-red-200',
                status ? 'dark:text-green-900' : 'dark:text-red-900'
            )}
        >
            {label}
        </span>
    );
};
