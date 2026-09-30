import {AlertDialogAction} from '@/components/ui/alert-dialog';
import {ComponentProps} from 'react';
import {twMerge} from 'tailwind-merge';

type DestructiveAlertDialogActionPropsType = ComponentProps<typeof AlertDialogAction>;

const DestructiveAlertDialogAction = ({className, ...props}: DestructiveAlertDialogActionPropsType) => (
    <AlertDialogAction
        className={twMerge(
            'bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active',
            className
        )}
        {...props}
    />
);

DestructiveAlertDialogAction.displayName = 'DestructiveAlertDialogAction';

export default DestructiveAlertDialogAction;
