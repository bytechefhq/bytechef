import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {ensureMonacoConfigured, getMonacoInstance, isMonacoConfigured} from '@/shared/components/monacoSetup';
import useIsDarkMode from '@/shared/hooks/useIsDarkMode';
import {MONACO_DARK_THEME, MONACO_LIGHT_THEME, defineMonacoDarkTheme} from '@/shared/util/monacoTheme-utils';
import {DiffEditor} from '@monaco-editor/react';
import {useEffect, useState} from 'react';

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
        <DiffEditor
            className={className}
            language={language}
            loading={<MonacoEditorLoader />}
            modified={modified}
            options={DIFF_EDITOR_OPTIONS}
            original={original}
            theme={isDarkMode ? MONACO_DARK_THEME : MONACO_LIGHT_THEME}
        />
    );
};

export default MonacoDiffEditorWrapper;
