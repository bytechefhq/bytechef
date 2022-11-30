import {PropsWithChildren} from 'react';
import IntegrationItemFooter from './IntegrationItemFooter';
import IntegrationItemHeader from './IntegrationItemHeader';

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
		<>
			<IntegrationItemHeader
				name={name}
				status={status}
				dropdownTrigger={dropdownTrigger}
			/>
			<IntegrationItemFooter
				category={category}
				tag={tag}
				button={button}
				date={date}
			/>
		</>
	);
};
