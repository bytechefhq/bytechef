import {PropsWithChildren} from 'react';

type Props = {
	dropdownTrigger: string;
};

export const IntegrationItemDropdownTrigger: React.FC<
	PropsWithChildren<Props>
> = ({dropdownTrigger}) => {
	return <span>{dropdownTrigger}</span>;
};
