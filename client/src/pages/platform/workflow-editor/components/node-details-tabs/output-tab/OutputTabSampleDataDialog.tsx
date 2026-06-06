import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {EDITOR_PLACEHOLDER, SPACE} from '@/shared/constants';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {SparklesIcon} from 'lucide-react';
import {Suspense, lazy, useCallback, useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {useSampleOutputCopilot} from './hooks/useSampleOutputCopilot';

import type {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface OutputTabSampleDataDialogProps {
    onClose: () => void;
    onUpload: (value: string) => void;
    open: boolean;
    placeholder?: object;
    workflowId?: string;
    workflowNodeName?: string;
}

const OutputTabSampleDataDialog = ({
    onClose,
    onUpload,
    open,
    placeholder,
    workflowId,
    workflowNodeName,
}: OutputTabSampleDataDialogProps) => {
    const [parsedValue, setParsedValue] = useState<object | undefined>();
    const [rawValue, setRawValue] = useState<string>('');

    const sampleOutputRef = useRef<object | undefined>(undefined);

    const ai = useApplicationInfoStore((state) => state.ai);
    const ff1570 = useFeatureFlagsStore()('ff-1570');

    const applyValue = useCallback((value: string) => {
        setRawValue(value);

        try {
            const parsed = JSON.parse(value);

            setParsedValue(parsed);
            sampleOutputRef.current = parsed;
        } catch {
            setParsedValue(undefined);
            sampleOutputRef.current = undefined;
        }
    }, []);

    const {copilotPanelOpen, handleCopilotClose, handleCopilotOpen} = useSampleOutputCopilot({
        onApply: applyValue,
        sampleOutputRef,
        workflowId,
        workflowNodeName,
    });

    const copilotAvailable = ai.copilot.enabled && ff1570;

    const handleEditorOnChange = (editorValue: string | undefined) => {
        const placeholderElement = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (placeholderElement) {
            placeholderElement.style.display = editorValue ? 'none' : 'block';
        }

        setRawValue(editorValue ?? '');

        if (editorValue) {
            try {
                const parsed = JSON.parse(editorValue);

                setParsedValue(parsed);
                sampleOutputRef.current = parsed;
            } catch {
                setParsedValue(undefined);
                sampleOutputRef.current = undefined;
            }
        } else {
            setParsedValue(undefined);
            sampleOutputRef.current = undefined;
        }
    };

    const handleEditorOnMount = (editor: StandaloneCodeEditorType) => {
        const placeholderElement = document.querySelector('#monaco-placeholder') as HTMLElement | null;

        if (placeholderElement) {
            placeholderElement.style.display = rawValue ? 'none' : 'block';
        }

        editor.focus();
    };

    const handleOpenChange = (isOpen: boolean) => {
        if (!isOpen) {
            const hasPlaceholder = placeholder !== undefined && Object.keys(placeholder).length > 0;

            setRawValue(hasPlaceholder ? JSON.stringify(placeholder, null, SPACE) : '');
            setParsedValue(hasPlaceholder ? placeholder : undefined);
            sampleOutputRef.current = hasPlaceholder ? placeholder : undefined;

            if (copilotAvailable) {
                handleCopilotClose();
            }

            onClose();
        }
    };

    useEffect(() => {
        if (placeholder !== undefined && Object.keys(placeholder).length) {
            const stringified = JSON.stringify(placeholder, null, SPACE);

            setRawValue(stringified);
            setParsedValue(placeholder);
            sampleOutputRef.current = placeholder;
        } else {
            setRawValue('');
            setParsedValue(undefined);
            sampleOutputRef.current = undefined;
        }
    }, [placeholder]);

    return (
        <Dialog modal={false} onOpenChange={handleOpenChange} open={open}>
            <DialogContent
                className={twMerge(
                    'flex flex-row gap-0 overflow-hidden p-0',
                    copilotAvailable && copilotPanelOpen
                        ? 'max-w-output-tab-sample-data-dialog-width sm:max-w-output-tab-sample-data-dialog-width'
                        : 'max-w-[800px] sm:max-w-[800px]'
                )}
                onInteractOutside={(event) => event.preventDefault()}
            >
                <div className="flex min-h-output-tab-sample-data-dialog-height min-w-0 flex-1 flex-col p-6">
                    <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                        <div className="flex flex-col space-y-1">
                            <DialogTitle>Upload Sample Output Data</DialogTitle>

                            <DialogDescription>
                                Chat with the assistant or edit the JSON directly. Click Upload when you&apos;re done.
                            </DialogDescription>
                        </div>

                        <div className="flex items-center gap-1">
                            {copilotAvailable && (
                                <Button
                                    aria-label="Copilot"
                                    icon={<SparklesIcon className="size-4" />}
                                    onClick={copilotPanelOpen ? handleCopilotClose : handleCopilotOpen}
                                    size="icon"
                                    variant="ghost"
                                />
                            )}

                            <DialogCloseButton />
                        </div>
                    </DialogHeader>

                    <div className="relative mt-4 flex-1">
                        <div className="absolute inset-0">
                            <Suspense fallback={<MonacoEditorLoader />}>
                                <MonacoEditor
                                    className="bg-transparent"
                                    defaultLanguage="json"
                                    onChange={handleEditorOnChange}
                                    onMount={handleEditorOnMount}
                                    value={rawValue}
                                />
                            </Suspense>

                            <div
                                className="pointer-events-none absolute top-[-2px] left-[70px] h-full text-sm text-muted-foreground"
                                id="monaco-placeholder"
                            >
                                {EDITOR_PLACEHOLDER}
                            </div>
                        </div>
                    </div>

                    <div className="mt-4 flex justify-end">
                        <Button
                            disabled={!parsedValue}
                            label="Upload"
                            onClick={() => {
                                if (parsedValue) {
                                    onUpload(JSON.stringify(parsedValue));
                                }
                            }}
                            type="submit"
                        />
                    </div>
                </div>

                {copilotAvailable && (
                    <div className="flex">
                        <CopilotPanel
                            className="h-full border-l border-border/50"
                            onClose={handleCopilotClose}
                            open={copilotPanelOpen}
                        />
                    </div>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default OutputTabSampleDataDialog;
