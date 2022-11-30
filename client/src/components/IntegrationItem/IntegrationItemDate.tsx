import React, {PropsWithChildren} from 'react';

type Props = {
	date: string;
};

export const IntegrationItemDate: React.FC<PropsWithChildren<Props>> = ({
	date,
}) => {
	return <span>{date}</span>;
};
