import React, {PropsWithChildren} from 'react';

type Props = {
	name: string;
};

export const IntegrationItemName: React.FC<PropsWithChildren<Props>> = ({
	name,
}) => {
	return <span>{name}</span>;
};
