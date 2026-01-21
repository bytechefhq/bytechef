import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {EDITOR_PLACEHOLDER, SPACE} from '@/shared/constants';
import {Suspense, lazy, useEffect, useState} from 'react';

import type {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface OutputTabSampleDataDialogProps {
    onClose: () => void;
    onUpload: (value: string) => void;
    open: boolean;
    placeholder?: object;
}

const OutputTabSampleDataDialog = ({onClose, onUpload, open, placeholder}: OutputTabSampleDataDialogProps) => {
    const [rawValue, setRawValue] = useState<string>('');
    const [parsedValue, setParsedValue] = useState<object | undefined>();

    const handleEditorOnChange = (editorValue: string | undefined) => {
        const placeholderElement = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (placeholderElement) {
            placeholderElement.style.display = editorValue ? 'none' : 'block';
        }

        setRawValue(editorValue ?? '');

        if (editorValue) {
            try {
                setParsedValue(JSON.parse(editorValue));
            } catch {
                setParsedValue(undefined);
            }
        } else {
            setParsedValue(undefined);
        }
    };

    const handleEditorOnMount = (editor: StandaloneCodeEditorType) => {
        const placeholderElement = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (placeholderElement) {
            placeholderElement.style.display = rawValue ? 'none' : 'block';
        }

        editor.focus();
    };

    const handleOpenChange = (isOpen: boolean) => {
        if (!isOpen) {
            const hasPlaceholder = placeholder !== undefined && Object.keys(placeholder).length > 0;

            setRawValue(hasPlaceholder ? JSON.stringify(placeholder, null, SPACE) : '');
            setParsedValue(hasPlaceholder ? placeholder : undefined);
            onClose();
        }
    };

    useEffect(() => {
        if (placeholder !== undefined && Object.keys(placeholder).length) {
            const stringified = JSON.stringify(placeholder, null, SPACE);

            setRawValue(stringified);
            setParsedValue(placeholder);
        } else {
            setRawValue('');
            setParsedValue(undefined);
        }
    }, [placeholder]);

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogContent className="max-w-output-tab-sample-data-dialog-width">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Upload Sample Output Data</DialogTitle>

                        <DialogDescription>
                            Add sample value in JSON format. Click Upload when you&apos;re done.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="relative mt-4 min-h-output-tab-sample-data-dialog-height flex-1">
                    <div className="absolute inset-0">
                        <Suspense fallback={<MonacoEditorLoader />}>
                            <MonacoEditor
                                className="bg-transparent"
                                defaultLanguage="json"
                                onChange={handleEditorOnChange}
                                onMount={handleEditorOnMount}
                                value={rawValue}
                            />
                        </Suspense>

                        <div
                            className="pointer-events-none absolute left-[70px] top-[-2px] h-full text-sm text-muted-foreground"
                            id="monaco-placeholder"
                        >
                            {EDITOR_PLACEHOLDER}
                        </div>
                    </div>
                </div>

                <DialogFooter>
                    <Button
                        disabled={!parsedValue}
                        label="Upload"
                        onClick={() => {
                            if (parsedValue) {
                                onUpload(JSON.stringify(parsedValue));
                            }
                        }}
                        type="submit"
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default OutputTabSampleDataDialog;
