import React from 'react';

export const Date: React.FC<{date?: Date}> = ({date}) => {
    return <div>Last modified {date && date.toLocaleDateString()}</div>;
};
