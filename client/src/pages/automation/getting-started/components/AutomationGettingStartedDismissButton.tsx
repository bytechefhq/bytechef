import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {useGettingStartedStore} from '@/shared/stores/useGettingStartedStore';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

const AutomationGettingStartedDismissButton = () => {
    const [showConfirmation, setShowConfirmation] = useState(false);

    const complete = useGettingStartedStore((state) => state.complete);

    const navigate = useNavigate();

    const handleConfirm = () => {
        complete('automation', 'DISMISSED');

        setShowConfirmation(false);

        void navigate('/automation/projects');
    };

    return (
        <>
            <Button label="Dismiss" onClick={() => setShowConfirmation(true)} size="sm" variant="ghost" />

            <AlertDialog
                confirmButtonVariant="default"
                confirmLabel="Dismiss Getting Started"
                description="Getting Started leaves the sidebar and Automation opens on Projects. It leaves every workspace. Nothing you built is removed."
                onCancel={() => setShowConfirmation(false)}
                onConfirm={handleConfirm}
                open={showConfirmation}
                title="Dismiss Getting Started?"
            />
        </>
    );
};

export default AutomationGettingStartedDismissButton;
