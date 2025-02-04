import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {EDITOR_PLACEHOLDER, SPACE} from '@/shared/constants';
import Editor from '@monaco-editor/react';
import {editor} from 'monaco-editor';
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;

import React, {useEffect, useState} from 'react';

const OutputTabSampleDataDialog = ({
    onClose,
    onUpload,
    open,
    sampleOutput,
}: {
    onClose: () => void;
    onUpload: (value: string) => void;
    open: boolean;
    sampleOutput?: object;
}) => {
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

                /* eslint-disable @typescript-eslint/no-unused-vars */
            } catch (e) {
                // thrown if value is not valid JSON
            }
        }
    };

    const handleEditorOnMount = (editor: IStandaloneCodeEditor) => {
        const placeholder = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (!placeholder) {
            return;
        }

        placeholder.style.display = value ? 'none' : 'block';

        editor.focus();
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            setValue(sampleOutput);
            onClose();
        }
    };

    useEffect(() => {
        setValue(sampleOutput !== undefined && Object.keys(sampleOutput).length ? sampleOutput : undefined);
    }, [sampleOutput]);

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
                        <Editor
                            className="bg-transparent"
                            defaultLanguage="json"
                            onChange={handleEditorOnChange}
                            onMount={handleEditorOnMount}
                            value={JSON.stringify(value, null, SPACE)}
                        />

                        <div
                            className="absolute left-[70px] top-[-2px] h-full text-sm text-muted-foreground"
                            id="monaco-placeholder"
                        >
                            {EDITOR_PLACEHOLDER}
                        </div>
                    </div>
                </div>

                <DialogFooter>
                    <Button
                        disabled={!value}
                        onClick={() => {
                            if (value) {
                                onUpload(value && JSON.stringify(value));
                            }
                        }}
                        type="submit"
                    >
                        Upload
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default OutputTabSampleDataDialog;
