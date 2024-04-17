import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import LayoutContainer from '@/layouts/LayoutContainer';
import PageHeader from '@/layouts/PageHeader';
import SigningKeyDialog from '@/pages/platform/settings/embedded/signing-keys/components/SigningKeyDialog';
import SigningKeyTable from '@/pages/platform/settings/embedded/signing-keys/components/SigningKeyTable';
import {useGeSigningKeysQuery} from '@/queries/embedded/signingKeys.queries';
import {ClipboardSignatureIcon} from 'lucide-react';

const SigningKeys = () => {
    const {data: signingKeys, error: signingKeysError, isLoading: signingKeysLoading} = useGeSigningKeysQuery();

    return (
        <PageLoader errors={[signingKeysError]} loading={signingKeysLoading}>
            <LayoutContainer
                header={
                    <PageHeader
                        className="w-full px-4 2xl:w-4/5"
                        position="main"
                        right={
                            signingKeys &&
                            signingKeys.length > 0 && (
                                <SigningKeyDialog triggerNode={<Button>New Signing Key</Button>} />
                            )
                        }
                        title="Signing Keys"
                    />
                }
                leftSidebarOpen={false}
            >
                {signingKeys && signingKeys.length > 0 ? (
                    <SigningKeyTable signingKeys={signingKeys} />
                ) : (
                    <EmptyList
                        button={<SigningKeyDialog triggerNode={<Button>New Signing Key</Button>} />}
                        className="2xl:w-4/5"
                        icon={<ClipboardSignatureIcon className="size-12 text-gray-400" />}
                        message="Get started by creating a new Signing key."
                        title="No Signing Keys"
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default SigningKeys;
