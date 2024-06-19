import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import SigningKeyDialog from '@/pages/settings/embedded/signing-keys/components/SigningKeyDialog';
import SigningKeyTable from '@/pages/settings/embedded/signing-keys/components/SigningKeyTable';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useGeSigningKeysQuery} from '@/shared/queries/embedded/signingKeys.queries';
import {ClipboardSignatureIcon} from 'lucide-react';
import {useState} from 'react';

const SigningKeys = () => {
    const [showEditDialog, setShowEditDialog] = useState(false);

    const {data: signingKeys, error: signingKeysError, isLoading: signingKeysLoading} = useGeSigningKeysQuery();

    return (
        <PageLoader errors={[signingKeysError]} loading={signingKeysLoading}>
            <LayoutContainer
                header={
                    <Header
                        className="w-full px-4 2xl:mx-auto 2xl:w-4/5"
                        position="main"
                        right={
                            signingKeys &&
                            signingKeys.length > 0 &&
                            signingKeys && (
                                <SigningKeyDialog
                                    remainingEnvironments={signingKeys.map((signingKey) =>
                                        signingKey.environment!.toString()
                                    )}
                                    triggerNode={<Button>New Signing Key</Button>}
                                />
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
                        button={<Button onClick={() => setShowEditDialog(true)}>New Signing Key</Button>}
                        icon={<ClipboardSignatureIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new Signing key."
                        title="No Signing Keys"
                    />
                )}

                {showEditDialog && signingKeys && (
                    <SigningKeyDialog
                        onClose={() => setShowEditDialog(false)}
                        remainingEnvironments={signingKeys.map((signingKey) => signingKey.environment!.toString())}
                    />
                )}
            </LayoutContainer>
        </PageLoader>
    );
};

export default SigningKeys;
