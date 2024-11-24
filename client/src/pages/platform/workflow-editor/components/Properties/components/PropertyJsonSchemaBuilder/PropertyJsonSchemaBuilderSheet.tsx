import JsonSchemaBuilder from '@/components/JsonSchemaBuilder/JsonSchemaBuilder';
import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import Editor from '@monaco-editor/react';
import React from 'react';

interface PropertyJsonSchemaBuilderSheetProps {
    locale?: string;
    onChange?: (newSchema: SchemaRecordType) => void;
    onClose?: () => void;
    schema: SchemaRecordType;
}

const PropertyJsonSchemaBuilderSheet = ({locale, onChange, onClose, schema}: PropertyJsonSchemaBuilderSheetProps) => {
    return (
        <Sheet onOpenChange={onClose} open>
            <SheetContent
                className="flex w-11/12 flex-col gap-0 p-0 sm:max-w-screen-lg"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <Tabs className="flex size-full flex-col" defaultValue="designer">
                    <SheetHeader>
                        <div className="p-4">
                            <div className="mr-8 flex items-center justify-between">
                                <div className="flex flex-col">
                                    <SheetTitle>JSON Schema Builder</SheetTitle>

                                    <SheetHeader>
                                        Define desired response format for the output returned by the model
                                    </SheetHeader>
                                </div>

                                <TabsList>
                                    <TabsTrigger value="designer">Designer</TabsTrigger>

                                    <TabsTrigger value="editor">Editor</TabsTrigger>
                                </TabsList>
                            </div>
                        </div>
                    </SheetHeader>

                    <div className="flex-1 overflow-y-auto px-4">
                        <TabsContent className="" value="designer">
                            <JsonSchemaBuilder locale={locale} onChange={onChange} schema={schema} />
                        </TabsContent>

                        <TabsContent className="h-full" value="editor">
                            <Editor
                                defaultLanguage="json"
                                onChange={(value) => {
                                    if (value && onChange) {
                                        onChange(JSON.parse(value));
                                    }
                                }}
                                value={JSON.stringify(schema, null, 4)}
                            />
                        </TabsContent>
                    </div>
                </Tabs>
            </SheetContent>
        </Sheet>
    );
};

export default PropertyJsonSchemaBuilderSheet;
