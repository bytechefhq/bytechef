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
import {useState} from 'react';

const sampleOutput = {
    country: 'USA',
    people: [
        {
            age: 28,
            firstName: 'Joe',
            gender: 'male',
            lastName: 'Jackson',
            number: '7349282382',
        },
    ],
};

const OutputTabSampleDataDialog = ({
    onClose,
    onUpload,
    open,
}: {
    open: boolean;
    onClose: () => void;
    onUpload: (value: string) => void;
}) => {
    const [value, setValue] = useState<string>(JSON.stringify(sampleOutput));

    return (
        <Dialog onOpenChange={(open) => !open && onClose()} open={open}>
            <DialogContent className="max-w-output-tab-sample-data-dialog-width">
                <DialogHeader>
                    <div className="flex items-center justify-between">
                        <DialogTitle>Upload Sample Output Data</DialogTitle>
                    </div>

                    <DialogDescription>
                        Add sample value in JSON format. Click Upload when you&apos;re done.
                    </DialogDescription>
                </DialogHeader>

                <div className="min-h-output-tab-sample-data-dialog-height relative mt-4 flex-1">
                    <div className="absolute inset-0">
                        <Editor
                            defaultLanguage="json"
                            onChange={(value) => value && setValue(value)}
                            value={JSON.stringify(sampleOutput, null, 4)}
                        />
                    </div>
                </div>

                <DialogFooter>
                    <Button
                        onClick={() => {
                            if (value) {
                                onUpload(value);
                                setValue(JSON.stringify(sampleOutput));
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
