import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Label} from '@/components/ui/label';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {EDITOR_PLACEHOLDER, SPACE} from '@/shared/constants';
import {Suspense, lazy, useEffect, useState} from 'react';

import type {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface OutputTabSampleDataDialogProps {
    fileUploadEnabled?: boolean;
    onClose: () => void;
    onFileUpload?: (file: File) => void;
    onUpload: (value: string) => void;
    open: boolean;
    placeholder?: object;
}

const OutputTabSampleDataDialog = ({
    fileUploadEnabled = false,
    onClose,
    onFileUpload,
    onUpload,
    open,
    placeholder,
}: OutputTabSampleDataDialogProps) => {
    const [rawValue, setRawValue] = useState<string>('');
    const [parsedValue, setParsedValue] = useState<object | undefined>();
    const [sampleFile, setSampleFile] = useState<File | null>(null);

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
            setSampleFile(null);
            onClose();
        }
    };

    const handleUploadClick = () => {
        if (fileUploadEnabled) {
            if (sampleFile && onFileUpload) {
                onFileUpload(sampleFile);
            }
        } else if (parsedValue) {
            onUpload(JSON.stringify(parsedValue));
        }
    };

    const uploadDisabled = fileUploadEnabled ? !sampleFile : !parsedValue;

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
                        description={
                            fileUploadEnabled
                                ? "Choose a file to use as this step's output. Click Upload when you're done."
                                : "Add sample value in JSON format. Click Upload when you're done."
                        }
                        title="Upload Sample Output Data"
                    />

                    <DialogBody>
                        {fileUploadEnabled ? (
                            <fieldset className="mt-4 flex flex-col gap-1.5 border-0">
                                <Label htmlFor="sampleFile">Sample file</Label>

                                <Input
                                    className="bg-background"
                                    id="sampleFile"
                                    onChange={(event) => setSampleFile(event.target.files?.[0] || null)}
                                    type="file"
                                />
                            </fieldset>
                        ) : (
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
                        )}
                    </DialogBody>

                    <DialogFooter>
                        <Button disabled={uploadDisabled} label="Upload" onClick={handleUploadClick} type="submit" />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default OutputTabSampleDataDialog;
