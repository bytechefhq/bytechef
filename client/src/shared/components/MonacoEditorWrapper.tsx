import LoadingIcon from '@/components/LoadingIcon';
import {editor} from 'monaco-editor';
import {Suspense, lazy} from 'react';

// Lazy load the actual Monaco Editor
const Editor = lazy(() => import('@monaco-editor/react'));

export type StandaloneCodeEditorType = editor.IStandaloneCodeEditor;

interface MonacoEditorProps {
    className?: string;
    defaultLanguage: string;
    onChange: (value: string | undefined) => void;
    onMount: (editor: StandaloneCodeEditorType) => void;
    options?: Record<string, object>;
    value?: string;
}

// Wrap the Editor with Suspense
const MonacoEditorWrapper = (props: MonacoEditorProps) => (
    <Suspense fallback={<MonacoEditorLoader />}>
        <Editor {...props} />
    </Suspense>
);

export const MonacoEditorLoader = () => (
    <div className="flex h-full items-center justify-center p-4">
        <div className="flex items-center gap-2">
            <LoadingIcon className="size-4 animate-spin text-muted-foreground" />

            <span className="text-muted-foreground">Loading editor...</span>
        </div>
    </div>
);

export default MonacoEditorWrapper;
