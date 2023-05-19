import {Cross1Icon} from '@radix-ui/react-icons';
import Button from 'components/Button/Button';
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
    <div className="absolute z-50 w-full rounded-md border bg-gray-100 shadow-md">
        <header className="flex items-center justify-between border-b-2 px-4 py-2">
            <span className="font-medium">{title}</span>

            <Button
                displayType="icon"
                icon={<Cross1Icon />}
                size="small"
                onClick={handleCancelClick}
            />
        </header>

        <main className="p-4">{children}</main>

        <footer className="flex items-center justify-end space-x-2 border-t-2 px-4 py-2">
            <Button
                displayType="secondary"
                label={cancelButtonLabel}
                onClick={handleCancelClick}
                size="small"
            />

            <Button
                displayType="primary"
                label={saveButtonLabel}
                onClick={handleSaveClick}
                size="small"
            />
        </footer>
    </div>
);

export default ContextualDialog;
