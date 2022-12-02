import React, {PropsWithChildren} from 'react';

type Props = {
	category: string;
};

export const Category: React.FC<PropsWithChildren<Props>> = ({category}) => {
	return <div>{category}</div>;
};
