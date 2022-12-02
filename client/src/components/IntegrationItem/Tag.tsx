import React, {PropsWithChildren} from 'react';

type Props = {
	tag: string;
};

export const Tag: React.FC<PropsWithChildren<Props>> = ({tag}) => {
	return <div>{tag}</div>;
};
