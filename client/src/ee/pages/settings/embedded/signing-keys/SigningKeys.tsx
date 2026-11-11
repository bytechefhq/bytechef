import Button from '@/components/Button/Button';
import EmptyState from '@/components/EmptyState/EmptyState';
import PageLoader from '@/components/PageLoader';
import {EMBEDDED_SIGNING_KEYS_DOCUMENTATION_URL} from '@/ee/pages/embedded/embeddedDocumentationUrls';
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
                        description="Sign the user tokens your backend hands to the frontend SDK, so ByteChef knows which connected user is calling."
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
                    <EmptyState
                        actions={<Button onClick={() => setShowEditDialog(true)}>New Signing Key</Button>}
                        description="Your backend signs a short-lived token for each user with a signing key, and the frontend SDK passes it to ByteChef to open the Connect popup."
                        docsLink={{href: EMBEDDED_SIGNING_KEYS_DOCUMENTATION_URL, label: 'How signing keys work'}}
                        icon={<ClipboardSignatureIcon />}
                        title="No Signing Keys"
                    />
                )}

                {showEditDialog && signingKeys && <SigningKeyDialog onClose={() => setShowEditDialog(false)} />}
            </LayoutContainer>
        </PageLoader>
    );
};

export default SigningKeys;
