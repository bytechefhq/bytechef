import {SidebarContentLayout} from '../../components/Layouts/SidebarContentLayout';
import {PageHeader} from 'components/PageHeader/PageHeader';
import {IntegrationItem} from 'components/IntegrationItem/IntegrationItem';
import {IntegrationList} from './IntegrationList';

export const Integrations = (): JSX.Element => {
	return (
		<SidebarContentLayout title={'Integrations'}>
			<PageHeader
				subTitle="All Integrations"
				buttonLabel="New Integration"
			/>

			<IntegrationItem
				name="NAME"
				status="STATUS"
				dropdownTrigger="DROPDOWNTRIGGER"
				category="CATEGORY"
				tag="TAG"
				button="BUTTON"
				date="DATE"
			/>

			{<IntegrationList />}
		</SidebarContentLayout>
	);
};
