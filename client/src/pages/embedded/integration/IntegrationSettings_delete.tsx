import IntegrationSettingsConnectionForm from '@/pages/embedded/integration/components/delete/IntegrationSettingsConnectionForm';
import IntegrationSettingsDeleteForm from '@/pages/embedded/integration/components/delete/IntegrationSettingsDeleteForm';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

const IntegrationSettings_delete = () => {
    return (
        <LayoutContainer header={<Header position="main" title="Settings" />} leftSidebarOpen={false}>
            <div className="flex w-5/12 flex-col gap-y-20 p-4">
                <IntegrationSettingsConnectionForm />

                <IntegrationSettingsDeleteForm />
            </div>
        </LayoutContainer>
    );
};

export default IntegrationSettings_delete;
