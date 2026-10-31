import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {ensureMonacoConfigured, getMonacoInstance, isMonacoConfigured} from '@/shared/components/monacoSetup';
import useIsDarkMode from '@/shared/hooks/useIsDarkMode';
import {MONACO_DARK_THEME, MONACO_LIGHT_THEME, defineMonacoDarkTheme} from '@/shared/util/monacoTheme-utils';
import Editor from '@monaco-editor/react';
import {useEffect, useState} from 'react';

import type {EditorOptionsType, StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';
import type {editor} from 'monaco-editor';

interface MonacoEditorProps {
    className?: string;
    defaultLanguage: string;
    onChange: (value: string | undefined) => void;
    onMount: (editor: StandaloneCodeEditorType) => void;
    onValidate?: (markers: editor.IMarkerData[]) => void;
    options?: EditorOptionsType;
    value?: string;
}

const MonacoEditorWrapper = (props: MonacoEditorProps) => {
    const [isReady, setIsReady] = useState(isMonacoConfigured());

    const isDarkMode = useIsDarkMode();

    useEffect(() => {
        if (!isReady) {
            ensureMonacoConfigured().then(() => setIsReady(true));
        }
    }, [isReady]);

    useEffect(() => {
        const monacoInstance = getMonacoInstance();

        if (isReady && isDarkMode && monacoInstance) {
            defineMonacoDarkTheme(monacoInstance);
        }
    }, [isDarkMode, isReady]);

    if (!isReady) {
        return <MonacoEditorLoader />;
    }

    return (
        <Editor
            {...props}
            loading={<MonacoEditorLoader />}
            theme={isDarkMode ? MONACO_DARK_THEME : MONACO_LIGHT_THEME}
        />
    );
};

export default MonacoEditorWrapper;
