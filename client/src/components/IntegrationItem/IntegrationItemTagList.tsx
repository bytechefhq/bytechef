import {PropsWithChildren} from 'react';
import {IntegrationItemButton} from './IntegrationItemButton';
import {IntegrationItemTag} from './IntegrationItemTag';

type Props = {
	tag: string;
	button: string;
};

export const IntegrationItemTagList: React.FC<PropsWithChildren<Props>> = ({
	tag,
	button,
}) => {
	return (
		<>
			<IntegrationItemTag tag={tag} />
			<IntegrationItemButton button={button} />
		</>
	);
};
