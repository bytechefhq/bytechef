import AlertDialog from '@/components/AlertDialog';
import Button from '@/components/Button/Button';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useDismissAutomationGettingStartedMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

const AutomationGettingStartedDismissButton = () => {
    const [showConfirmation, setShowConfirmation] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const dismissMutation = useDismissAutomationGettingStartedMutation({
        onSuccess: () => {
            setShowConfirmation(false);

            void queryClient.invalidateQueries({queryKey: ['automationGettingStartedCompleted']});
            void queryClient.invalidateQueries({queryKey: ['automationGettingStarted']});

            void navigate('/automation/projects');
        },
    });

    return (
        <>
            <Button label="Dismiss" onClick={() => setShowConfirmation(true)} size="sm" variant="ghost" />

            <AlertDialog
                confirmButtonVariant="default"
                confirmLabel="Dismiss Getting Started"
                description="Getting Started leaves the sidebar and Automation opens on Projects. It leaves every workspace. Nothing you built is removed."
                isPending={dismissMutation.isPending}
                onCancel={() => setShowConfirmation(false)}
                onConfirm={() => dismissMutation.mutate({workspaceId: `${currentWorkspaceId}`})}
                open={showConfirmation}
                title="Dismiss Getting Started?"
            />
        </>
    );
};

export default AutomationGettingStartedDismissButton;
