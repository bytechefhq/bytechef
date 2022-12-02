import React, {PropsWithChildren} from 'react';
import {Category} from './Category';
import {Date} from './Date';
import {TagList} from './TagList';

type Props = {
	category: string;
	tag: string;
	button: string;
	date: string;
};

const Footer: React.FC<PropsWithChildren<Props>> = ({
	category,
	tag,
	button,
	date,
}) => {
	return (
		<div>
			<Category category={category} />
			<TagList tag={tag} button={button} />
			<Date date={date} />
		</div>
	);
};

export default Footer;
