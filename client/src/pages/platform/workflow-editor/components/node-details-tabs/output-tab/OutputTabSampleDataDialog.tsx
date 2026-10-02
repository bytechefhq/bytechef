import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
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
        <Dialog modal={false} onOpenChange={handleOpenChange} open={open}>
            <DialogContent onInteractOutside={(event) => event.preventDefault()} size="lg">
                <DialogMain>
                    <DialogHeader
                        description="Add sample value in JSON format. Click Upload when you're done."
                        title="Upload Sample Output Data"
                    />

                    <DialogBody>
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
                                    className="pointer-events-none absolute top-[-2px] left-[70px] h-full text-sm text-muted-foreground"
                                    id="monaco-placeholder"
                                >
                                    {EDITOR_PLACEHOLDER}
                                </div>
                            </div>
                        </div>
                    </DialogBody>

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
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default OutputTabSampleDataDialog;
