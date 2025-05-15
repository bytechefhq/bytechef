import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import React from 'react';

import {SchemaRecordType} from '../utils/types';
import SchemaMenu from './SchemaMenu';

interface SchemaMenuModalProps {
    onChange: (schema: SchemaRecordType) => void;
    onClose: () => void;
    schema: SchemaRecordType;
    title?: string | React.ReactNode;
}

const SchemaMenuDialog = ({onChange, onClose, schema, title}: SchemaMenuModalProps) => (
    <Dialog onOpenChange={onClose} open={true}>
        <DialogContent>
            <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                <div className="flex flex-col space-y-1">
                    <DialogTitle>{title}</DialogTitle>

                    <DialogDescription>Define additional fields</DialogDescription>
                </div>

                <DialogCloseButton />
            </DialogHeader>

            <SchemaMenu onChange={onChange} schema={schema} />
        </DialogContent>
    </Dialog>
);

export default SchemaMenuDialog;
