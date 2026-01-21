import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import SigningKeyDialog from '@/ee/pages/settings/embedded/signing-keys/components/SigningKeyDialog';
import SigningKeyTable from '@/ee/pages/settings/embedded/signing-keys/components/SigningKeyTable';
import {useGeSigningKeysQuery} from '@/ee/shared/queries/embedded/signingKeys.queries';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
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
                        centerTitle
                        position="main"
                        right={
                            signingKeys &&
                            signingKeys.length > 0 &&
                            signingKeys && <SigningKeyDialog triggerNode={<Button>New Signing Key</Button>} />
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

                {showEditDialog && signingKeys && <SigningKeyDialog onClose={() => setShowEditDialog(false)} />}
            </LayoutContainer>
        </PageLoader>
    );
};

export default SigningKeys;
