import React from 'react';
import cx from 'classnames';

type ColorType = 'enabled' | 'disabled';

const color: Record<ColorType, string> = {
    enabled: 'green',
    disabled: 'red',
};

export const Status: React.FC<{status: boolean}> = ({status}) => {
    const label = status ? 'Enabled' : 'Disabled';

    const statusType = status ? 'enabled' : 'disabled';

    return (
        <span
            className={cx(
                'mr-2 rounded px-2.5 py-0.5 text-sm font-medium',
                `bg-${color[statusType]}-100`,
                `text-${color[statusType]}-800`,
                `dark:bg-${color[statusType]}-200`,
                `dark:text-${color[statusType]}-900`
            )}
        >
            {label}
        </span>
    );
};
