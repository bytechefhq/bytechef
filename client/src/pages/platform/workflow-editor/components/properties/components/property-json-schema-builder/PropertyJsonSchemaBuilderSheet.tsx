import JsonSchemaBuilder from '@/components/JsonSchemaBuilder/JsonSchemaBuilder';
import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {Note} from '@/components/Note';
import {Sheet, SheetCloseButton, SheetContent, SheetDescription, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {MonacoEditorLoader} from '@/shared/components/MonacoEditorWrapper';
import {SPACE} from '@/shared/constants';
import {MessageCircleQuestionIcon} from 'lucide-react';
import {Suspense, lazy} from 'react';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface PropertyJsonSchemaBuilderSheetProps {
    locale?: string;
    onChange?: (newSchema: SchemaRecordType) => void;
    onClose?: () => void;
    schema?: SchemaRecordType;
    title?: string;
}

const PropertyJsonSchemaBuilderSheet = ({
    locale,
    onChange,
    onClose,
    schema,
    title,
}: PropertyJsonSchemaBuilderSheetProps) => {
    return (
        <Sheet onOpenChange={onClose} open>
            <SheetContent
                className="flex w-11/12 flex-col gap-0 p-0 sm:max-w-screen-lg"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <Tabs className="flex size-full flex-col" defaultValue="designer">
                    <SheetHeader className="flex flex-row items-center justify-between space-y-0 p-3">
                        <div className="flex flex-col">
                            <SheetTitle>{`${title} Builder` || 'JSON Schema Builder'}</SheetTitle>

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
                                content="Define how you’d like the LLM to structure its responses — essentially a template for its output."
                                icon={<MessageCircleQuestionIcon />}
                            />
                        )}

                        <TabsContent value="designer">
                            <JsonSchemaBuilder locale={locale} onChange={onChange} schema={schema} />
                        </TabsContent>

                        <TabsContent className="h-full" value="editor">
                            <Suspense fallback={<MonacoEditorLoader />}>
                                <MonacoEditor
                                    className="size-full"
                                    defaultLanguage="json"
                                    onChange={(value) => {
                                        if (value && onChange) {
                                            try {
                                                onChange(JSON.parse(value));
                                            } catch (e) {
                                                console.error('Invalid JSON:', e);
                                            }
                                        }
                                    }}
                                    onMount={(editor) => {
                                        editor.focus();
                                    }}
                                    value={JSON.stringify(schema, null, SPACE)}
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
