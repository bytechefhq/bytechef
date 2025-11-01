import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {Suspense, lazy} from 'react';

import type {EditorOptionsType, StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';

const Editor = lazy(() => import('@monaco-editor/react'));

interface MonacoEditorProps {
    className?: string;
    defaultLanguage: string;
    onChange: (value: string | undefined) => void;
    onMount: (editor: StandaloneCodeEditorType) => void;
    options?: EditorOptionsType;
    value?: string;
}

const MonacoEditorWrapper = (props: MonacoEditorProps) => (
    <Suspense fallback={<MonacoEditorLoader />}>
        <Editor {...props} loading={<MonacoEditorLoader />} />
    </Suspense>
);

export default MonacoEditorWrapper;
