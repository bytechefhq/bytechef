import {PropsWithChildren} from 'react';

type Props = {
	button: string;
};
export const IntegrationItemButton: React.FC<PropsWithChildren<Props>> = ({
	button,
}) => {
	return <span>{button}</span>;
};
