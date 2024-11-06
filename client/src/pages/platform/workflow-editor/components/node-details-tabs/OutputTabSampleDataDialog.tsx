import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import Editor from '@monaco-editor/react';
import {editor} from 'monaco-editor';
import {useEffect, useState} from 'react';
import IStandaloneCodeEditor = editor.IStandaloneCodeEditor;

const placeholder = (
    <>
        <pre>{'//'}Write sample output value, for example:</pre>
        <pre>{'{'}</pre>
        <pre className="pl-4">{'"country": "USA"'}</pre>
        <pre className="pl-4">{'"people": ['}</pre>
        <pre className="pl-8">{'{'}</pre>
        <pre className="pl-12">{'"firstName": Joe'}</pre>
        <pre className="pl-12">{'"lastName": Jackson'}</pre>
        <pre className="pl-12">{'"gender": Male'}</pre>
        <pre className="pl-12">{'"age": 28'}</pre>
        <pre className="pl-12">{'"number": 7349282382'}</pre>
        <pre className="pl-8">{'}'}</pre>
        <pre className="pl-4">{']'}</pre>
        <pre>{'}'}</pre>
    </>
);

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
                <DialogHeader>
                    <div className="flex items-center justify-between">
                        <DialogTitle>Upload Sample Output Data</DialogTitle>
                    </div>

                    <DialogDescription>
                        Add sample value in JSON format. Click Upload when you&apos;re done.
                    </DialogDescription>
                </DialogHeader>

                <div className="relative mt-4 min-h-output-tab-sample-data-dialog-height flex-1">
                    <div className="absolute inset-0">
                        <Editor
                            className="bg-transparent"
                            defaultLanguage="json"
                            onChange={handleEditorOnChange}
                            onMount={handleEditorOnMount}
                            value={JSON.stringify(value, null, 4)}
                        />

                        <div
                            className="absolute left-[70px] top-[-2px] h-full text-sm text-muted-foreground"
                            id="monaco-placeholder"
                        >
                            {placeholder}
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
