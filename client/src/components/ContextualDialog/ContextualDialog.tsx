import {Button} from '@/components/ui/button';
import {Cross2Icon} from '@radix-ui/react-icons';
import {ReactNode} from 'react';

interface ContextualDialogProps {
    children: ReactNode;
    handleCancelClick: () => void;
    handleSaveClick: () => void;
    title: string;
    cancelButtonLabel?: string;
    saveButtonLabel?: string;
}

const ContextualDialog = ({
    cancelButtonLabel = 'Cancel',
    children,
    handleCancelClick,
    handleSaveClick,
    saveButtonLabel = 'Save',
    title,
}: ContextualDialogProps) => (
    <div className="absolute z-50 w-full rounded-md border bg-white shadow-md">
        <header className="flex items-center justify-between px-4 py-2">
            <span className="font-medium">{title}</span>

            <button onClick={handleCancelClick}>
                <Cross2Icon aria-hidden="true" className="size-4 cursor-pointer" />
            </button>
        </header>

        <main className="p-4">{children}</main>

        <footer className="flex items-center justify-end space-x-2 px-4 py-2">
            <Button onClick={handleCancelClick} size="sm" variant="secondary">
                {cancelButtonLabel}
            </Button>

            <Button onClick={handleSaveClick} size="sm">
                {saveButtonLabel}
            </Button>
        </footer>
    </div>
);

export default ContextualDialog;
