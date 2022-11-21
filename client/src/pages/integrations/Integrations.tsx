import {SidebarContentLayout} from '../../components/Layouts/SidebarContentLayout';
import {PageHeader} from 'components/PageHeader/PageHeader';

export const Integrations = (): JSX.Element => {
	return (
		<SidebarContentLayout title={'Integrations'}>
			<PageHeader />
			{/* <IntegrationList /> */}
		</SidebarContentLayout>
	);
};
