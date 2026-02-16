import PrimaryButton from '@/components/Button/Button';
import DeleteIdentityProviderAlertDialog from '@/ee/pages/settings/platform/identity-providers/components/DeleteIdentityProviderAlertDialog';
import IdentityProviderDialog from '@/ee/pages/settings/platform/identity-providers/components/IdentityProviderDialog';
import IdentityProvidersTable from '@/ee/pages/settings/platform/identity-providers/components/IdentityProvidersTable';
import useIdentityProviderDialog from '@/ee/pages/settings/platform/identity-providers/components/hooks/useIdentityProviderDialog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';

export default function IdentityProvidersPage() {
    const {handleOpenCreate} = useIdentityProviderDialog();

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle
                    description="Configure OIDC and SAML identity providers for Single Sign-On."
                    position="main"
                    right={<PrimaryButton onClick={handleOpenCreate}>Add Identity Provider</PrimaryButton>}
                    title="Identity Providers"
                />
            }
            leftSidebarOpen={false}
        >
            <div className="w-full space-y-4 px-4 text-sm 3xl:mx-auto 3xl:w-4/5">
                <IdentityProvidersTable />
            </div>

            <DeleteIdentityProviderAlertDialog />

            <IdentityProviderDialog />
        </LayoutContainer>
    );
}
