import Button from '@/components/Button/Button';
import JsonSchemaBuilder from '@/components/JsonSchemaBuilder/JsonSchemaBuilder';
import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {Note} from '@/components/Note';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {SPACE} from '@/shared/constants';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {MessageCircleQuestionIcon, SparklesIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';
import {Suspense, lazy, useCallback, useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {usePropertyJsonSchemaBuilderCopilot} from './hooks/usePropertyJsonSchemaBuilderCopilot';

import type {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface PropertyJsonSchemaBuilderSheetProps {
    environmentId?: number;
    onChange?: (newSchema: SchemaRecordType) => void;
    onClose?: () => void;
    propertyPath?: string;
    schema?: SchemaRecordType;
    title?: string;
    workflowId?: string;
    workflowNodeName?: string;
}

const PropertyJsonSchemaBuilderSheet = ({
    environmentId,
    onChange,
    onClose,
    propertyPath,
    schema,
    title,
    workflowId,
    workflowNodeName,
}: PropertyJsonSchemaBuilderSheetProps) => {
    const [localSchema, setLocalSchema] = useState<SchemaRecordType | undefined>(schema);

    const editorRef = useRef<StandaloneCodeEditorType | null>(null);
    const schemaRef = useRef(localSchema);

    schemaRef.current = localSchema;

    const ai = useApplicationInfoStore((state) => state.ai);
    const ff1570 = useFeatureFlagsStore()('ff-1570');

    const copilotAvailable = Boolean(
        ai.copilot.enabled && ff1570 && workflowId && workflowNodeName && environmentId !== undefined
    );

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

    const {copilotPanelOpen, handleCopilotClose, handleCopilotOpen} = usePropertyJsonSchemaBuilderCopilot({
        onSchemaApply: handleSchemaChange,
        propertyPath,
        schemaRef,
        title,
        workflowId,
        workflowNodeName,
    });

    // The sheet's own close (the outside-click/Escape/close-icon path Radix drives through
    // onOpenChange) bypasses handleCopilotClose entirely, since that only runs when the Copilot panel
    // itself closes. Without this, opening Copilot inside the sheet and then closing the sheet directly
    // pushes a conversation-stack entry that never gets popped. handleCopilotClose is token-guarded, so
    // calling it here is a no-op on the (common) case where Copilot was never opened.
    const handleSheetOpenChange = useCallback(
        (open: boolean) => {
            if (!open) {
                handleCopilotClose();
            }

            onClose?.();
        },
        [handleCopilotClose, onClose]
    );

    useEffect(() => {
        setLocalSchema(schema);
    }, [schema]);

    return (
        <Sheet onOpenChange={handleSheetOpenChange} open>
            <VisuallyHidden.Root>
                <SheetTitle>{title ? `${title} Builder` : 'JSON Schema Builder'}</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className={twMerge(
                    'top-3 right-4 bottom-4 flex h-auto w-11/12 flex-row gap-0 rounded-md bg-surface-neutral-secondary p-0',
                    copilotPanelOpen ? 'sm:max-w-(--breakpoint-xl)' : 'sm:max-w-(--breakpoint-lg)'
                )}
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <div className="flex min-w-0 flex-1 flex-col">
                    <Tabs className="flex size-full flex-col" defaultValue="designer" onValueChange={handleTabChange}>
                        <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
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

                                {copilotAvailable && (
                                    <Button
                                        aria-label="Copilot"
                                        icon={<SparklesIcon className="size-4" />}
                                        onClick={handleCopilotOpen}
                                        size="icon"
                                        variant="ghost"
                                    />
                                )}

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
                </div>

                {copilotAvailable && (
                    <CopilotPanel
                        className="h-full rounded-r-md border-l border-l-border/50"
                        onClose={handleCopilotClose}
                        open={copilotPanelOpen}
                    />
                )}
            </SheetContent>
        </Sheet>
    );
};

export default PropertyJsonSchemaBuilderSheet;
