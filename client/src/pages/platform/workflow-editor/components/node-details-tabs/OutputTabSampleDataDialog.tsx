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
        <div>{'//'}Write sample output value, for example:</div>
        <div>{'{'}</div>
        <div className="pl-4">{'"country": "USA"'}</div>
        <div className="pl-4">{'"people": ['}</div>
        <div className="pl-8">{'{'}</div>
        <div className="pl-12">{'"firstName": Joe'}</div>
        <div className="pl-12">{'"lastName": Jackson'}</div>
        <div className="pl-12">{'"gender": Male'}</div>
        <div className="pl-12">{'"age": 28'}</div>
        <div className="pl-12">{'"number": 7349282382'}</div>
        <div className="pl-8">{'}'}</div>
        <div className="pl-4">{']'}</div>
        <div>{'}'}</div>
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

        if (value) {
            placeholder!.style.display = 'none';
        } else {
            placeholder!.style.display = 'block';
        }

        if (value != null) {
            try {
                setValue(JSON.parse(value));

                /* eslint-disable @typescript-eslint/no-unused-vars */
            } catch (e) {
                // Do nothing
            }
        }
    };

    const handleEditorOnMount = (editor: IStandaloneCodeEditor) => {
        const placeholder = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (value) {
            placeholder!.style.display = 'none';
        } else {
            placeholder!.style.display = 'block';
        }

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
            <DialogContent className="max-w-[800px]">
                <DialogHeader>
                    <div className="flex items-center justify-between">
                        <DialogTitle>Upload Sample Output Data</DialogTitle>
                    </div>

                    <DialogDescription>
                        Add sample value in JSON format. Click Upload when you&apos;re done.
                    </DialogDescription>
                </DialogHeader>

                <div className="relative mt-4 min-h-[400px] flex-1">
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
