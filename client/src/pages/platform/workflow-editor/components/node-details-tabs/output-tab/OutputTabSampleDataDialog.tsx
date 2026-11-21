import Button from '@/components/Button/Button';
import {Dialog, DialogBody, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Label} from '@/components/ui/label';
import {Skeleton} from '@/components/ui/skeleton';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {EDITOR_PLACEHOLDER, SPACE} from '@/shared/constants';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {SparklesIcon} from 'lucide-react';
import {Suspense, lazy, useCallback, useEffect, useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {useSampleOutputCopilot} from './hooks/useSampleOutputCopilot';

import type {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const SKELETON_LINE_WIDTHS = ['w-4', 'w-2/5', 'w-1/3', 'w-2/5', 'w-1/4', 'w-4'];

const SampleDataEditorSkeleton = () => (
    <div
        aria-busy="true"
        className="absolute inset-0 flex flex-col gap-2 py-1 pl-[70px]"
        data-testid="sample-data-editor-skeleton"
    >
        {SKELETON_LINE_WIDTHS.map((width, index) => (
            <Skeleton className={twMerge('h-4', width)} key={index} />
        ))}
    </div>
);

interface OutputTabSampleDataDialogProps {
    fileUploadEnabled?: boolean;
    onClose: () => void;
    onFileUpload?: (file: File) => void;
    onUpload: (value: string) => void;
    open: boolean;
    placeholder?: object;
    workflowId?: string;
    workflowNodeName?: string;
}

const OutputTabSampleDataDialog = ({
    fileUploadEnabled = false,
    onClose,
    onFileUpload,
    onUpload,
    open,
    placeholder,
    workflowId,
    workflowNodeName,
}: OutputTabSampleDataDialogProps) => {
    const [parsedValue, setParsedValue] = useState<object | undefined>();
    const [rawValue, setRawValue] = useState<string>('');
    const [sampleFile, setSampleFile] = useState<File | null>(null);
    const [isEditorMounted, setIsEditorMounted] = useState(false);

    const sampleOutputRef = useRef<object | undefined>(undefined);

    const ai = useApplicationInfoStore((state) => state.ai);

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

    const copilotAvailable = ai.copilot.enabled && !fileUploadEnabled;

    const handleEditorOnChange = (editorValue: string | undefined) => {
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
        setIsEditorMounted(true);

        editor.focus();
    };

    const handleOpenChange = (isOpen: boolean) => {
        if (!isOpen) {
            const hasPlaceholder = placeholder !== undefined && Object.keys(placeholder).length > 0;

            setRawValue(hasPlaceholder ? JSON.stringify(placeholder, null, SPACE) : '');
            setParsedValue(hasPlaceholder ? placeholder : undefined);
            setSampleFile(null);
            setIsEditorMounted(false);
            sampleOutputRef.current = hasPlaceholder ? placeholder : undefined;

            if (copilotAvailable) {
                handleCopilotClose();
            }

            onClose();
        }
    };

    const handleUploadClick = () => {
        if (fileUploadEnabled) {
            if (sampleFile && onFileUpload) {
                onFileUpload(sampleFile);
            }
        } else if (parsedValue) {
            onUpload(JSON.stringify(parsedValue));
        }
    };

    const uploadDisabled = fileUploadEnabled ? !sampleFile : !parsedValue;

    const showEditorSkeleton = !isEditorMounted && !!rawValue;

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
                onInteractOutside={(event) => event.preventDefault()}
                size={copilotAvailable && copilotPanelOpen ? 'xl' : 'lg'}
            >
                <DialogMain>
                    <DialogHeader
                        description={
                            fileUploadEnabled
                                ? "Choose a file to use as this step's output. Click Upload when you're done."
                                : "Chat with the assistant or edit the JSON directly. Click Upload when you're done."
                        }
                        endContent={
                            copilotAvailable && (
                                <Button
                                    aria-label="Copilot"
                                    icon={<SparklesIcon className="size-4" />}
                                    onClick={copilotPanelOpen ? handleCopilotClose : handleCopilotOpen}
                                    size="icon"
                                    variant="ghost"
                                />
                            )
                        }
                        title="Upload Sample Output Data"
                    />

                    <DialogBody>
                        {fileUploadEnabled ? (
                            <fieldset className="mt-4 flex flex-col gap-1.5 border-0">
                                <Label htmlFor="sampleFile">Sample file</Label>

                                <Input
                                    className="bg-background"
                                    id="sampleFile"
                                    onChange={(event) => setSampleFile(event.target.files?.[0] || null)}
                                    type="file"
                                />
                            </fieldset>
                        ) : (
                            <div className="relative mt-4 min-h-output-tab-sample-data-dialog-height flex-1">
                                {showEditorSkeleton && <SampleDataEditorSkeleton />}

                                <div className={twMerge('absolute inset-0', showEditorSkeleton && 'invisible')}>
                                    <Suspense fallback={<MonacoEditorLoader />}>
                                        <MonacoEditor
                                            className="bg-transparent"
                                            defaultLanguage="json"
                                            onChange={handleEditorOnChange}
                                            onMount={handleEditorOnMount}
                                            value={rawValue}
                                        />
                                    </Suspense>

                                    {isEditorMounted && !rawValue && (
                                        <div
                                            className="pointer-events-none absolute top-[-2px] left-[70px] h-full text-sm text-muted-foreground"
                                            id="monaco-placeholder"
                                        >
                                            {EDITOR_PLACEHOLDER}
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}
                    </DialogBody>

                    <DialogFooter>
                        <Button disabled={uploadDisabled} label="Upload" onClick={handleUploadClick} type="submit" />
                    </DialogFooter>
                </DialogMain>

                {copilotAvailable && (
                    <CopilotPanel
                        className="h-full rounded-r-lg border-l border-stroke-neutral-primary"
                        onClose={handleCopilotClose}
                        open={copilotPanelOpen}
                    />
                )}
            </DialogContent>
        </Dialog>
    );
};

export default OutputTabSampleDataDialog;
