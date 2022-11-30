import {PropsWithChildren} from 'react';

type Props = {
	status: string;
};

export const IntegrationItemStatus: React.FC<PropsWithChildren<Props>> = ({
	status,
}) => {
	return <span>{status}</span>;
};
