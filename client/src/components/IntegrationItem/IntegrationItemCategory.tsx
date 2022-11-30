import {PropsWithChildren} from 'react';

type Props = {
	category: string;
};

export const IntegrationItemCategory: React.FC<PropsWithChildren<Props>> = ({
	category,
}) => {
	return <span>{category}</span>;
};
