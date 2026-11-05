import {type ReactElement, type ReactNode} from 'react';

import {DialogBody, DialogHeader, DialogMain} from './DialogMain';
import {DialogSidebar} from './DialogSidebar';
import {DialogStepIndicator} from './DialogStepIndicator';
import {DialogSteps} from './DialogSteps';
import {DialogStepsProvider, type DialogStepsProviderProps} from './DialogStepsProvider';

interface DialogWizardProps extends Omit<DialogStepsProviderProps, 'children'> {
    children: ReactNode;
    description?: string;
    footer: ReactNode;
    icon?: ReactElement;
    title: string;
}

function DialogWizard({children, description, footer, icon, title, ...stepsProviderProps}: DialogWizardProps) {
    return (
        <DialogStepsProvider {...stepsProviderProps}>
            <DialogSidebar description={description} icon={icon} title={title}>
                <DialogSteps />

                <DialogStepIndicator />
            </DialogSidebar>

            <DialogMain>
                <DialogHeader dialogDescription={description} dialogTitle={title} />

                <DialogBody>{children}</DialogBody>

                {footer}
            </DialogMain>
        </DialogStepsProvider>
    );
}

DialogWizard.displayName = 'DialogWizard';

export {DialogWizard};
export type {DialogWizardProps};
