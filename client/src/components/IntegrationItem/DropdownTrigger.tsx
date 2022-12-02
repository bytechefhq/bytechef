import React, {PropsWithChildren} from 'react';

type Props = {
	dropdownTrigger: string;
};

export const DropdownTrigger: React.FC<PropsWithChildren<Props>> = ({
	dropdownTrigger,
}) => {
	return <div>{dropdownTrigger}</div>;
};
