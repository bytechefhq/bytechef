import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {useDismissEmbeddedGettingStartedMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const EmbeddedGettingStartedDismissButton = () => {
    const [showConfirmation, setShowConfirmation] = useState(false);

    const queryClient = useQueryClient();

    const dismissMutation = useDismissEmbeddedGettingStartedMutation({
        onSuccess: () => {
            setShowConfirmation(false);

            void queryClient.invalidateQueries({queryKey: ['embeddedGettingStartedCompleted']});
            void queryClient.invalidateQueries({queryKey: ['embeddedGettingStarted']});
        },
    });

    return (
        <>
            <Button label="Dismiss" onClick={() => setShowConfirmation(true)} size="sm" variant="ghost" />

            <AlertDialog
                confirmButtonVariant="default"
                confirmLabel="Dismiss Getting Started"
                description="Getting Started leaves the sidebar and Embedded opens where it usually does. You can still open this page later from Help & Feedback in the sidebar."
                isPending={dismissMutation.isPending}
                onCancel={() => setShowConfirmation(false)}
                onConfirm={() => dismissMutation.mutate({})}
                open={showConfirmation}
                title="Dismiss Getting Started?"
            />
        </>
    );
};

export default EmbeddedGettingStartedDismissButton;
