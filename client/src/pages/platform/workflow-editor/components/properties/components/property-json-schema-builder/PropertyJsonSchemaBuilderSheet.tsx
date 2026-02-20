import JsonSchemaBuilder from '@/components/JsonSchemaBuilder/JsonSchemaBuilder';
import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {Note} from '@/components/Note';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {SPACE} from '@/shared/constants';
import {MessageCircleQuestionIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';
import {Suspense, lazy, useCallback, useState} from 'react';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface PropertyJsonSchemaBuilderSheetProps {
    onChange?: (newSchema: SchemaRecordType) => void;
    onClose?: () => void;
    schema?: SchemaRecordType;
    title?: string;
}

const PropertyJsonSchemaBuilderSheet = ({onChange, onClose, schema, title}: PropertyJsonSchemaBuilderSheetProps) => {
    const [localSchema, setLocalSchema] = useState<SchemaRecordType | undefined>(schema);

    const handleSchemaChange = useCallback(
        (newSchema: SchemaRecordType) => {
            setLocalSchema(newSchema);

            onChange?.(newSchema);
        },
        [onChange]
    );

    return (
        <Sheet onOpenChange={onClose} open>
            <VisuallyHidden.Root>
                <SheetTitle>{title ? `${title} Builder` : 'JSON Schema Builder'}</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="absolute bottom-4 right-4 top-3 flex h-auto w-11/12 flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-screen-lg"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <Tabs className="flex size-full flex-col" defaultValue="designer">
                    <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md bg-surface-neutral-primary p-3">
                        <div className="flex flex-col">
                            <span className="text-lg font-semibold">
                                {title ? `${title} Builder` : 'JSON Schema Builder'}
                            </span>

                            <span className="text-sm text-muted-foreground">{`Define desired structure for the ${title}.`}</span>
                        </div>

                        <div className="flex items-center gap-1">
                            <TabsList>
                                <TabsTrigger value="designer">Designer</TabsTrigger>

                                <TabsTrigger value="editor">Code Editor</TabsTrigger>
                            </TabsList>

                            <SheetCloseButton />
                        </div>
                    </header>

                    <div className="flex-1 space-y-4 overflow-y-auto p-3">
                        {title === 'Response Schema' && (
                            <Note
                                content="Define how you'd like the LLM to structure its responses — essentially a template for its output."
                                icon={<MessageCircleQuestionIcon />}
                            />
                        )}

                        <TabsContent value="designer">
                            <JsonSchemaBuilder onChange={handleSchemaChange} schema={localSchema} />
                        </TabsContent>

                        <TabsContent className="h-full" value="editor">
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
                                        editor.focus();
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
