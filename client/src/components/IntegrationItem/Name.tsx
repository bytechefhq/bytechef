import React, {PropsWithChildren} from 'react';

type Props = {
	name: string;
};

export const Name: React.FC<PropsWithChildren<Props>> = ({name}) => {
	return <div>{name}</div>;
};
