import React from 'react';

export const Status: React.FC<{status: string}> = ({status}) => {
    return <div>{status}</div>;
};
