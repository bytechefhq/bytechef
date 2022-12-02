import React, {PropsWithChildren} from 'react';
import {Button} from './Button';
import {Tag} from './Tag';

type Props = {
	tag: string;
	button: string;
};

export const TagList: React.FC<PropsWithChildren<Props>> = ({tag, button}) => {
	return (
		<div>
			<Tag tag={tag} />
			<Button button={button} />
		</div>
	);
};
