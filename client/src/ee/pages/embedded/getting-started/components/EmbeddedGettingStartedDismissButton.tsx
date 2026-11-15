import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {useGettingStartedStore} from '@/shared/stores/useGettingStartedStore';
import {useState} from 'react';

const EmbeddedGettingStartedDismissButton = () => {
    const [showConfirmation, setShowConfirmation] = useState(false);

    const complete = useGettingStartedStore((state) => state.complete);

    const handleConfirm = () => {
        complete('embedded', 'DISMISSED');

        setShowConfirmation(false);
    };

    return (
        <>
            <Button label="Dismiss" onClick={() => setShowConfirmation(true)} size="sm" variant="ghost" />

            <AlertDialog
                confirmButtonVariant="default"
                confirmLabel="Dismiss Getting Started"
                description="Getting Started leaves the sidebar and Embedded opens where it usually does. You can still open this page later from Help & Feedback in the sidebar."
                onCancel={() => setShowConfirmation(false)}
                onConfirm={handleConfirm}
                open={showConfirmation}
                title="Dismiss Getting Started?"
            />
        </>
    );
};

export default EmbeddedGettingStartedDismissButton;
