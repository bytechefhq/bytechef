import {PropsWithChildren} from 'react';
import {IntegrationItemDropdownTrigger} from './IntegrationItemDropdownTrigger';
import {IntegrationItemName} from './IntegrationItemName';
import {IntegrationItemStatus} from './IntegrationItemStatus';

type Props = {
	name: string;
	status: string;
	dropdownTrigger: string;
};

const IntegrationItemHeader: React.FC<PropsWithChildren<Props>> = ({
	name,
	status,
	dropdownTrigger,
}) => {
	return (
		<>
			<IntegrationItemName name={name} />
			<IntegrationItemStatus status={status} />
			<IntegrationItemDropdownTrigger dropdownTrigger={dropdownTrigger} />
		</>
	);
};

export default IntegrationItemHeader;
