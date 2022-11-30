import {PropsWithChildren} from 'react';

type Props = {
	tag: string;
};

export const IntegrationItemTag: React.FC<PropsWithChildren<Props>> = ({
	tag,
}) => {
	return <span>{tag}</span>;
};
