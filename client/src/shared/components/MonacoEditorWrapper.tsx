import LoadingDots from '@/components/LoadingDots';
import {editor} from 'monaco-editor';
import {Suspense, lazy} from 'react';

const Editor = lazy(() => import('@monaco-editor/react'));

export type StandaloneCodeEditorType = editor.IStandaloneCodeEditor;

interface MonacoEditorProps {
    className?: string;
    defaultLanguage: string;
    onChange: (value: string | undefined) => void;
    onMount: (editor: StandaloneCodeEditorType) => void;
    options?: editor.IStandaloneEditorConstructionOptions;
    value?: string;
}

const MonacoEditorWrapper = (props: MonacoEditorProps) => (
    <Suspense fallback={<MonacoEditorLoader />}>
        <Editor {...props} loading={<MonacoEditorLoader />} />
    </Suspense>
);

export const MonacoEditorLoader = () => (
    <div className="flex size-full items-center justify-center p-4">
        <LoadingDots />
    </div>
);

export default MonacoEditorWrapper;
