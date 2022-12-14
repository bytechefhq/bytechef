import React, {PropsWithChildren} from 'react';

type Props = {
    date: string;
};

export const Date: React.FC<{date: string}> = ({date}) => {
    return <div>{date}</div>;
};
