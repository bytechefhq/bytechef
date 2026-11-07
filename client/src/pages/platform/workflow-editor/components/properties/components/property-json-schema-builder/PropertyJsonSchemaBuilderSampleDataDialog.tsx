import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {Note} from '@/components/Note';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {TriangleAlertIcon} from 'lucide-react';
import {Suspense, lazy, useCallback, useMemo, useState} from 'react';

import {generateSchemaFromSample} from './utils/generateSchemaFromSample';

import type {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const GENERATION_ERROR_MESSAGE = 'Could not generate a schema from this sample. Please try again.';

const OVERWRITE_WARNING_MESSAGE = 'Generating will replace your current schema.';

interface PropertyJsonSchemaBuilderSampleDataDialogProps {
    onGenerate: (newSchema: SchemaRecordType) => void;
    onOpenChange: (open: boolean) => void;
    open: boolean;
    schema?: SchemaRecordType;
}

const PropertyJsonSchemaBuilderSampleDataDialog = ({
    onGenerate,
    onOpenChange,
    open,
    schema,
}: PropertyJsonSchemaBuilderSampleDataDialogProps) => {
    const [errorMessage, setErrorMessage] = useState<string | undefined>();
    const [generating, setGenerating] = useState(false);
    const [sample, setSample] = useState('');

    const sampleIsValidJson = useMemo(() => {
        if (!sample.trim()) {
            return false;
        }

        try {
            JSON.parse(sample);

            return true;
        } catch {
            return false;
        }
    }, [sample]);

    const schemaHasProperties = useMemo(
        () => Object.keys((schema?.properties as SchemaRecordType | undefined) ?? {}).length > 0,
        [schema]
    );

    const handleEditorMount = useCallback((editor: StandaloneCodeEditorType) => {
        editor.focus();
    }, []);

    const handleEditorChange = useCallback((value: string | undefined) => {
        setSample(value ?? '');
        setErrorMessage(undefined);
    }, []);

    const handleGenerateClick = useCallback(async () => {
        setGenerating(true);
        setErrorMessage(undefined);

        try {
            const newSchema = await generateSchemaFromSample(sample);

            onGenerate(newSchema);

            setSample('');

            onOpenChange(false);
        } catch {
            setErrorMessage(GENERATION_ERROR_MESSAGE);
        } finally {
            setGenerating(false);
        }
    }, [onGenerate, onOpenChange, sample]);

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent onInteractOutside={(event) => event.preventDefault()} size="lg">
                <DialogMain className="min-h-[600px]">
                    <DialogHeader
                        description="Paste a sample JSON payload. Its structure is used to infer the schema."
                        title="Generate Schema From Sample"
                    />

                    <DialogBody className="flex flex-col">
                        {schemaHasProperties && (
                            <Note content={OVERWRITE_WARNING_MESSAGE} icon={<TriangleAlertIcon />} />
                        )}

                        <div className="relative flex-1 overflow-hidden rounded-md border border-border/50">
                            <div className="absolute inset-0">
                                <Suspense fallback={<MonacoEditorLoader />}>
                                    <MonacoEditor
                                        className="size-full"
                                        defaultLanguage="json"
                                        onChange={handleEditorChange}
                                        onMount={handleEditorMount}
                                        options={{
                                            automaticLayout: true,
                                            folding: true,
                                            fontSize: 12,
                                            lineNumbers: 'on',
                                            minimap: {enabled: false},
                                            scrollBeyondLastLine: false,
                                            tabSize: 2,
                                            wordWrap: 'on',
                                        }}
                                        value={sample}
                                    />
                                </Suspense>
                            </div>
                        </div>

                        {errorMessage && (
                            <p className="text-sm text-destructive" role="alert">
                                {errorMessage}
                            </p>
                        )}
                    </DialogBody>

                    <DialogFooter>
                        <Button
                            disabled={!sampleIsValidJson || generating}
                            label={generating ? 'Generating...' : 'Generate'}
                            onClick={handleGenerateClick}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default PropertyJsonSchemaBuilderSampleDataDialog;
