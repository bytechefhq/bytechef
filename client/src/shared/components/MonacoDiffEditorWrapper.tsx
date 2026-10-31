import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {ensureMonacoConfigured, getMonacoInstance, isMonacoConfigured} from '@/shared/components/monacoSetup';
import useIsDarkMode from '@/shared/hooks/useIsDarkMode';
import {MONACO_DARK_THEME, MONACO_LIGHT_THEME, defineMonacoDarkTheme} from '@/shared/util/monacoTheme-utils';
import {DiffEditor, type MonacoDiffEditor} from '@monaco-editor/react';
import {useEffect, useRef, useState} from 'react';

interface MonacoDiffEditorProps {
    className?: string;
    language: string;
    modified: string;
    original: string;
}

const DIFF_EDITOR_OPTIONS = {
    minimap: {enabled: false},
    originalEditable: false,
    readOnly: true,
    renderSideBySide: true,
};

const MonacoDiffEditorWrapper = ({className, language, modified, original}: MonacoDiffEditorProps) => {
    const [isReady, setIsReady] = useState(isMonacoConfigured());

    const diffEditorRef = useRef<MonacoDiffEditor | null>(null);

    const isDarkMode = useIsDarkMode();

    const handleMount = (diffEditor: MonacoDiffEditor) => {
        diffEditorRef.current = diffEditor;
    };

    useEffect(() => {
        if (!isReady) {
            ensureMonacoConfigured()
                .then(() => setIsReady(true))
                .catch((error) => console.error('Failed to load the Monaco editor.', error));
        }
    }, [isReady]);

    useEffect(() => {
        const monacoInstance = getMonacoInstance();

        if (isReady && isDarkMode && monacoInstance) {
            defineMonacoDarkTheme(monacoInstance);
        }
    }, [isDarkMode, isReady]);

    useEffect(
        () => () => {
            const diffEditor = diffEditorRef.current;

            if (!diffEditor) {
                return;
            }

            const diffEditorModel = diffEditor.getModel();

            diffEditor.setModel(null);

            diffEditorModel?.original.dispose();
            diffEditorModel?.modified.dispose();

            diffEditorRef.current = null;
        },
        []
    );

    if (!isReady) {
        return <MonacoEditorLoader />;
    }

    return (
        <DiffEditor
            className={className}
            keepCurrentModifiedModel
            keepCurrentOriginalModel
            language={language}
            loading={<MonacoEditorLoader />}
            modified={modified}
            onMount={handleMount}
            options={DIFF_EDITOR_OPTIONS}
            original={original}
            theme={isDarkMode ? MONACO_DARK_THEME : MONACO_LIGHT_THEME}
        />
    );
};

export default MonacoDiffEditorWrapper;
