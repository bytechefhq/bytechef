import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {useGettingStartedStore} from '@/shared/stores/useGettingStartedStore';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

import {EMBEDDED_DEFAULT_ROUTE_PATH} from '../resolveEmbeddedIndexRedirect';

const EmbeddedGettingStartedDismissButton = () => {
    const [showConfirmation, setShowConfirmation] = useState(false);

    const complete = useGettingStartedStore((state) => state.complete);

    const navigate = useNavigate();

    const handleConfirm = () => {
        complete('embedded', 'DISMISSED');

        setShowConfirmation(false);

        void navigate(`/embedded/${EMBEDDED_DEFAULT_ROUTE_PATH}`);
    };

    return (
        <>
            <Button label="Dismiss" onClick={() => setShowConfirmation(true)} size="sm" variant="ghost" />

            <AlertDialog
                confirmButtonVariant="default"
                confirmLabel="Dismiss Getting Started"
                description="Getting Started leaves the sidebar and Embedded opens on Integrations. You can still open this page later from Help & Feedback in the sidebar."
                onCancel={() => setShowConfirmation(false)}
                onConfirm={handleConfirm}
                open={showConfirmation}
                title="Dismiss Getting Started?"
            />
        </>
    );
};

export default EmbeddedGettingStartedDismissButton;
