import {Button} from 'components/Button/Button';
import {PropsWithChildren} from 'react';

type Props = {
	subTitle: string;
	buttonTitle: string;
};

export const PageHeader: React.FC<PropsWithChildren<Props>> = ({
	subTitle,
	buttonTitle,
}) => {
	return (
		<div className="mb-6 flex justify-center py-4">
			<div className="flex w-full items-center justify-between">
				<h2 className="text-2xl tracking-tight text-gray-900 dark:text-gray-200">
					{subTitle}
				</h2>

				<div>
					<Button title={buttonTitle} />
				</div>
			</div>
		</div>
	);
};
