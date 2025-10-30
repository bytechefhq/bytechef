import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {EDITOR_PLACEHOLDER, SPACE} from '@/shared/constants';
import {getCookie} from '@/shared/util/cookie-utils';
import {Suspense, lazy, useState} from 'react';

import type {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const fetchGenerateSchema = async (data: string): Promise<Response> => {
    return await fetch('/api/platform/internal/generate-schema', {
        body: data,
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        method: 'POST',
    }).then((response) => response);
};

const PropertyJsonSchemaBuilderSampleDataDialog = ({onChange}: {onChange?: (newSchema: SchemaRecordType) => void}) => {
    const [curSchema, setCurSchema] = useState<SchemaRecordType | undefined>();
    const [open, setOpen] = useState<boolean>(true);

    const handleEditorOnChange = (value: string | undefined) => {
        const placeholder = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (!placeholder) {
            return;
        }

        placeholder.style.display = value ? 'none' : 'block';

        if (value != null) {
            try {
                setCurSchema(JSON.parse(value));

                /* eslint-disable @typescript-eslint/no-unused-vars */
            } catch (e) {
                // thrown if value is not valid JSON
            }
        }
    };

    const handleOnSubmit = () => {
        if (curSchema) {
            fetchGenerateSchema(JSON.stringify(curSchema)).then((response) => {
                if (response.ok) {
                    response.json().then((data) => {
                        if (onChange) {
                            onChange({...data});
                        }
                    });

                    setCurSchema(undefined);
                    setOpen(false);
                }
            });
        }
    };

    const handleEditorOnMount = (editor: StandaloneCodeEditorType) => {
        const placeholder = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (!placeholder) {
            return;
        }

        placeholder.style.display = curSchema ? 'none' : 'block';

        editor.focus();
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            setCurSchema(undefined);
        }

        setOpen(open);
    };

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogTrigger asChild>
                <Button variant="outline">Generate</Button>
            </DialogTrigger>

            <DialogContent className="max-w-output-tab-sample-data-dialog-width">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Sample JSON</DialogTitle>

                        <DialogDescription>Generate JSON schema from sample JSON</DialogDescription>
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
                                value={JSON.stringify(curSchema, null, SPACE)}
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
                    <Button disabled={!curSchema} onClick={handleOnSubmit} type="submit">
                        Generate
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default PropertyJsonSchemaBuilderSampleDataDialog;
