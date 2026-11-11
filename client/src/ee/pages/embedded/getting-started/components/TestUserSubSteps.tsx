import ConnectedUserCreateDialog from '@/ee/pages/embedded/connected-users/components/ConnectedUserCreateDialog';
import useConnectedUserSheetStore from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import SubStepList from '@/ee/pages/embedded/getting-started/components/SubStepList';
import {getTestUserSubSteps} from '@/ee/pages/embedded/getting-started/getEmbeddedGettingStartedSubSteps';
import {EmbeddedGettingStartedQuery} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

interface TestUserSubStepsProps {
    status: EmbeddedGettingStartedQuery['embeddedGettingStarted'];
}

const TestUserSubSteps = ({status}: TestUserSubStepsProps) => {
    const [createDialogOpen, setCreateDialogOpen] = useState(false);

    const setConnectedUserId = useConnectedUserSheetStore((state) => state.setConnectedUserId);
    const setConnectedUserSheetOpen = useConnectedUserSheetStore((state) => state.setConnectedUserSheetOpen);

    const queryClient = useQueryClient();

    const testUserId = status.testUserId != null ? Number(status.testUserId) : undefined;

    const subStepActions: Partial<Record<string, () => void>> = {
        add: () => setCreateDialogOpen(true),
        ...(testUserId !== undefined && {
            provision: () => {
                setConnectedUserId(testUserId);
                setConnectedUserSheetOpen(true);
            },
        }),
    };

    return (
        <>
            <SubStepList
                ariaLabel="Test user setup"
                subStepActions={subStepActions}
                subSteps={getTestUserSubSteps(status)}
            />

            {createDialogOpen && (
                <ConnectedUserCreateDialog
                    onClose={() => {
                        setCreateDialogOpen(false);

                        void queryClient.invalidateQueries({queryKey: ['embeddedGettingStarted']});
                    }}
                />
            )}
        </>
    );
};

export default TestUserSubSteps;
