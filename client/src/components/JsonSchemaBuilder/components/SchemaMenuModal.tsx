import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import React from 'react';

interface SchemaMenuModalProps {
    onClose: () => void;
    children?: React.ReactNode;
    title?: string | React.ReactNode;
}

const SchemaMenuModal = ({children, onClose, title}: SchemaMenuModalProps) => {
    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>{title}</DialogTitle>

                        <DialogDescription>Define additional fields</DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                {children}
            </DialogContent>
        </Dialog>
    );
};

export default SchemaMenuModal;
