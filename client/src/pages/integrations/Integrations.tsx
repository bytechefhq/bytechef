import {SidebarContentLayout} from '../../components/Layouts/SidebarContentLayout';
import {PageHeader} from 'components/PageHeader/PageHeader';
import {IntegrationList} from './IntegrationList';
import NewIntegrationModal from 'components/NewIntegrationModal/NewIntegrationModal';

export const Integrations = (): JSX.Element => {
	return (
		<SidebarContentLayout title={'Integrations'}>
			<PageHeader
				subTitle="All Integrations"
				buttonTitle="New Integration"
			/>
			<NewIntegrationModal />
			{/* <IntegrationList /> */}
		</SidebarContentLayout>
	);
};
