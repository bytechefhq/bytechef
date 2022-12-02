import React, {PropsWithChildren} from 'react';
import {DropdownTrigger} from './DropdownTrigger';
import {Name} from './Name';
import {Status} from './Status';

type Props = {
	name: string;
	status: string;
	dropdownTrigger: string;
};

const Header: React.FC<PropsWithChildren<Props>> = ({
	name,
	status,
	dropdownTrigger,
}) => {
	return (
		<div>
			<Name name={name} />
			<Status status={status} />
			<DropdownTrigger dropdownTrigger={dropdownTrigger} />
		</div>
	);
};

export default Header;
