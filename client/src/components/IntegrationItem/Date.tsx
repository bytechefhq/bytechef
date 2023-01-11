import React from 'react';

export const Date: React.FC<{date?: Date}> = ({date}) => {
    return <div>{date && date?.toString()}</div>;
};
