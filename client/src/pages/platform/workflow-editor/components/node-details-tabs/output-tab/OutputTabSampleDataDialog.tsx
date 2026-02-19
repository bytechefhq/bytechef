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
    const [value, setValue] = useState<object | undefined>();

    const handleEditorOnChange = (value: string | undefined) => {
        const placeholder = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (!placeholder) {
            return;
        }

        placeholder.style.display = value ? 'none' : 'block';

        if (value != null) {
            try {
                setValue(JSON.parse(value));
            } catch (error) {
                console.error('Invalid JSON:', error);
            }
        }
    };

    const handleEditorOnMount = (editor: StandaloneCodeEditorType) => {
        const monacoPlaceholder = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (!monacoPlaceholder) {
            return;
        }

        monacoPlaceholder.style.display = value ? 'none' : 'block';

        editor.focus();
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            setValue(placeholder);
            onClose();
        }
    };

    useEffect(() => {
        setValue(placeholder !== undefined && Object.keys(placeholder).length ? placeholder : undefined);
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
                                value={JSON.stringify(value, null, SPACE)}
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
                        disabled={!value}
                        label="Upload"
                        onClick={() => {
                            if (value) {
                                onUpload(value && JSON.stringify(value));
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
