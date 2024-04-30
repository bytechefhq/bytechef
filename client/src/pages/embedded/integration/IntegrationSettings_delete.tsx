import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import IntegrationSettingsConnectionForm from '@/pages/embedded/integration/components/delete/IntegrationSettingsConnectionForm';
import IntegrationSettingsDeleteForm from '@/pages/embedded/integration/components/delete/IntegrationSettingsDeleteForm';

const IntegrationSettings_delete = () => {
    return (
        <LayoutContainer header={<PageHeader position="main" title="Settings" />} leftSidebarOpen={false}>
            <div className="flex w-5/12 flex-col gap-y-20 p-4">
                <IntegrationSettingsConnectionForm />

                <IntegrationSettingsDeleteForm />
            </div>
        </LayoutContainer>
    );
};

export default IntegrationSettings_delete;
