import JsonSchemaBuilder from '@/components/JsonSchemaBuilder/JsonSchemaBuilder';
import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {Note} from '@/components/Note';
import {Sheet, SheetCloseButton, SheetContent, SheetDescription, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {SPACE} from '@/shared/constants';
import {MessageCircleQuestionIcon} from 'lucide-react';
import {Suspense, lazy, useCallback, useRef, useState} from 'react';

import type {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface PropertyJsonSchemaBuilderSheetProps {
    onChange?: (newSchema: SchemaRecordType) => void;
    onClose?: () => void;
    schema?: SchemaRecordType;
    title?: string;
}

const PropertyJsonSchemaBuilderSheet = ({onChange, onClose, schema, title}: PropertyJsonSchemaBuilderSheetProps) => {
    const [localSchema, setLocalSchema] = useState<SchemaRecordType | undefined>(schema);

    const editorRef = useRef<StandaloneCodeEditorType | null>(null);

    const handleSchemaChange = useCallback(
        (newSchema: SchemaRecordType) => {
            setLocalSchema(newSchema);

            onChange?.(newSchema);
        },
        [onChange]
    );

    const handleTabChange = useCallback((value: string) => {
        if (value === 'editor' && editorRef.current) {
            requestAnimationFrame(() => {
                editorRef.current?.layout();
                editorRef.current?.focus();
            });
        }
    }, []);

    return (
        <Sheet onOpenChange={onClose} open>
            <SheetContent
                className="flex w-11/12 flex-col gap-0 p-0 sm:max-w-screen-lg"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <Tabs className="flex size-full flex-col" defaultValue="designer" onValueChange={handleTabChange}>
                    <SheetHeader className="flex flex-row items-center justify-between space-y-0 p-3">
                        <div className="flex flex-col">
                            <SheetTitle>{title ? `${title} Builder` : 'JSON Schema Builder'}</SheetTitle>

                            <SheetDescription>{`Define desired structure for the ${title}.`}</SheetDescription>
                        </div>

                        <div className="flex items-center gap-1">
                            {/*TODO Fix refresh doesn't not work properly, backend does not always return correct schema*/}

                            {/*<PropertyJsonSchemaBuilderSampleDataDialog onChange={onChange} />*/}

                            <TabsList>
                                <TabsTrigger value="designer">Designer</TabsTrigger>

                                <TabsTrigger value="editor">Code Editor</TabsTrigger>
                            </TabsList>

                            <SheetCloseButton />
                        </div>
                    </SheetHeader>

                    <div className="flex-1 space-y-4 overflow-y-auto px-3">
                        {title === 'Response Schema' && (
                            <Note
                                content="Define how you'd like the LLM to structure its responses — essentially a template for its output."
                                icon={<MessageCircleQuestionIcon />}
                            />
                        )}

                        <TabsContent value="designer">
                            <JsonSchemaBuilder onChange={handleSchemaChange} schema={localSchema} />
                        </TabsContent>

                        <TabsContent className="h-full data-[state=inactive]:hidden" forceMount value="editor">
                            <Suspense fallback={<MonacoEditorLoader />}>
                                <MonacoEditor
                                    className="size-full"
                                    defaultLanguage="json"
                                    onChange={(value) => {
                                        if (value) {
                                            try {
                                                handleSchemaChange(JSON.parse(value));
                                            } catch {
                                                // Invalid JSON while typing — ignore until valid
                                            }
                                        }
                                    }}
                                    onMount={(editor) => {
                                        editorRef.current = editor;
                                    }}
                                    value={JSON.stringify(localSchema, null, SPACE)}
                                />
                            </Suspense>
                        </TabsContent>
                    </div>
                </Tabs>
            </SheetContent>
        </Sheet>
    );
};

export default PropertyJsonSchemaBuilderSheet;
