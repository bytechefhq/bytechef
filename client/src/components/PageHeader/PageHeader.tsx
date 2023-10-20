import NewIntegrationButton from './NewIntegrationButton';
import './NewIntegrationButton.css';

export const PageHeader = () => {
	return (
		<div className="mb-6 flex justify-center py-4">
			<div className="flex w-full items-center justify-between">
				<h2 className="text-2xl tracking-tight text-gray-900 dark:text-gray-200">
					All integrations
				</h2>

				<div>
					<NewIntegrationButton />
				</div>
			</div>
		</div>
	);
};
