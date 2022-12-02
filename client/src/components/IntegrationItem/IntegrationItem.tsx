import React, {PropsWithChildren} from 'react';
import Footer from './Footer';
import Header from './Header';

type Props = {
	name: string;
	status: string;
	dropdownTrigger: string;
	category: string;
	tag: string;
	button: string;
	date: string;
};

export const IntegrationItem: React.FC<PropsWithChildren<Props>> = ({
	name,
	status,
	dropdownTrigger,
	category,
	tag,
	button,
	date,
}) => {
	return (
		<div>
			<Header
				name={name}
				status={status}
				dropdownTrigger={dropdownTrigger}
			/>
			<Footer category={category} tag={tag} button={button} date={date} />
		</div>
	);
};
