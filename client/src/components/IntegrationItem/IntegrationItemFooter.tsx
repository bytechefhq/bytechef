import {PropsWithChildren} from 'react';
import {IntegrationItemCategory} from './IntegrationItemCategory';
import {IntegrationItemDate} from './IntegrationItemDate';
import {IntegrationItemTagList} from './IntegrationItemTagList';

type Props = {
	category: string;
	tag: string;
	button: string;
	date: string;
};

const IntegrationItemFooter: React.FC<PropsWithChildren<Props>> = ({
	category,
	tag,
	button,
	date,
}) => {
	return (
		<>
			<IntegrationItemCategory category={category} />
			<IntegrationItemTagList tag={tag} button={button} />
			<IntegrationItemDate date={date} />
		</>
	);
};

export default IntegrationItemFooter;
