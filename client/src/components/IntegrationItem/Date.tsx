import React, {PropsWithChildren} from 'react';

type Props = {
	date: string;
};

export const Date: React.FC<PropsWithChildren<Props>> = ({date}) => {
	return <div>{date}</div>;
};
