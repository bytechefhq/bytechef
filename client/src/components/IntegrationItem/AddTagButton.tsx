import React, {PropsWithChildren} from 'react';

type Props = {
	button: string;
};
export const AddTagButton: React.FC<PropsWithChildren<Props>> = ({button}) => {
	return <div>{button}</div>;
};
