import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
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
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>

                    <DialogDescription>Define additional fields</DialogDescription>
                </DialogHeader>

                {children}
            </DialogContent>
        </Dialog>
    );
};

export default SchemaMenuModal;
