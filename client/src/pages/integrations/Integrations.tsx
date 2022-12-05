import {SidebarContentLayout} from '../../components/Layouts/SidebarContentLayout';
import {PageHeader} from 'components/PageHeader/PageHeader';
import {IntegrationList} from './IntegrationList';

export const Integrations = (): JSX.Element => {
    return (
        <SidebarContentLayout title={'Integrations'}>
            <PageHeader
                subTitle="All Integrations"
                buttonTitle="New Integration"
            />
            <IntegrationList />
        </SidebarContentLayout>
    );
};
