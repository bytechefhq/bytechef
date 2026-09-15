import {useContext} from 'react';

import {DialogStepsContext, type DialogStepsContextI} from '../DialogStepsProvider';

export function useDialogSteps(): DialogStepsContextI {
    const context = useContext(DialogStepsContext);

    if (!context) {
        throw new Error('useDialogSteps must be used within a DialogStepsProvider');
    }

    return context;
}

export function useOptionalDialogSteps(): DialogStepsContextI | null {
    return useContext(DialogStepsContext);
}
