import React, {PropsWithChildren} from 'react';
import {AddTagButton} from './AddTagButton';
import {Tag} from './Tag';

type Props = {
	tag: string;
	button: string;
};

export const TagList: React.FC<PropsWithChildren<Props>> = ({tag, button}) => {
	return (
		<div>
			<Tag tag={tag} />
			<AddTagButton button={button} />
		</div>
	);
};
