import React, {PropsWithChildren} from 'react';

type Props = {
	status: string;
};

export const Status: React.FC<PropsWithChildren<Props>> = ({status}) => {
	return <div>{status}</div>;
};
