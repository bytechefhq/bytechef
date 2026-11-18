import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useCodeWorkflowHeaderStore from '@/pages/platform/code-workflow/stores/useCodeWorkflowHeaderStore';
import MonacoEditorWrapper from '@/shared/components/MonacoEditorWrapper';
import {CompletionContextType, ScriptLanguageType} from '@/shared/components/monaco/completion/completionTypes';
import {useCompletionContext} from '@/shared/components/monaco/completion/useCompletionContext';
import {useComponentCatalog} from '@/shared/components/monaco/completion/useComponentCatalog';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {CodeWorkflowLanguage} from '@/shared/middleware/graphql';
import getCodeWorkflowLanguageLabel from '@/shared/util/codeWorkflowLanguage-utils';
import {Settings2Icon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/shallow';

import type {editor} from 'monaco-editor';

const MONACO_LANGUAGE_BY_CODE_WORKFLOW_LANGUAGE: Record<CodeWorkflowLanguage, string> = {
    [CodeWorkflowLanguage.Java]: 'java',
    [CodeWorkflowLanguage.Javascript]: 'javascript',
    [CodeWorkflowLanguage.Python]: 'python',
    [CodeWorkflowLanguage.Ruby]: 'ruby',
};

const SCRIPT_MONACO_LANGUAGES = ['javascript', 'python', 'ruby'];

export interface CodeWorkflowSourceEditorProps {
    embedded?: boolean;
    error?: unknown;
    headerless?: boolean;
    isLoading: boolean;
    isSaving: boolean;
    language: string;
    onSave: (content: string) => Promise<unknown>;
    onTestConfigurationClick?: () => void;
    source?: string;
    testConfigurationDisabled?: boolean;
}

interface CodeWorkflowSourceEditorHeaderProps {
    isSaveDisabled: boolean;
    isSaving: boolean;
    language: string;
    onSave: () => void;
    onTestConfigurationClick?: () => void;
    testConfigurationDisabled?: boolean;
}

const CodeWorkflowSourceEditorHeader = ({
    isSaveDisabled,
    isSaving,
    language,
    onSave,
    onTestConfigurationClick,
    testConfigurationDisabled,
}: CodeWorkflowSourceEditorHeaderProps) => (
    <Header
        centerTitle
        position="main"
        right={
            <div className="flex items-center gap-1 pr-12">
                {onTestConfigurationClick && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span tabIndex={0}>
                                <Button
                                    aria-label="Test Configuration"
                                    disabled={testConfigurationDisabled}
                                    icon={<Settings2Icon />}
                                    onClick={onTestConfigurationClick}
                                    size="iconSm"
                                    variant="secondary"
                                />
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>
                            {testConfigurationDisabled
                                ? 'Declare a connection or an input in the source to enable test configuration.'
                                : 'Set the workflow test configuration.'}
                        </TooltipContent>
                    </Tooltip>
                )}

                <Button disabled={isSaveDisabled} label={isSaving ? 'Saving...' : 'Save'} onClick={onSave} size="sm" />
            </div>
        }
        title={
            <div className="flex items-center gap-2">
                <span>Code Workflow</span>

                <Badge label={getCodeWorkflowLanguageLabel(language)!} styleType="secondary-filled" weight="semibold" />
            </div>
        }
    />
);

const CodeWorkflowSourceEditor = ({
    embedded,
    error,
    headerless,
    isLoading,
    isSaving,
    language,
    onSave,
    onTestConfigurationClick,
    source,
    testConfigurationDisabled,
}: CodeWorkflowSourceEditorProps) => {
    const [isSourceDirty, setIsSourceDirty] = useState(false);
    const [model, setModel] = useState<editor.ITextModel>();

    const latestSourceRef = useRef(source ?? '');

    const {reset, setCodeWorkflowHeaderState} = useCodeWorkflowHeaderStore(
        useShallow((state) => ({
            reset: state.reset,
            setCodeWorkflowHeaderState: state.setCodeWorkflowHeaderState,
        }))
    );

    const monacoLanguage = MONACO_LANGUAGE_BY_CODE_WORKFLOW_LANGUAGE[language as CodeWorkflowLanguage];

    const components = useComponentCatalog({embedded});

    const completionContext = useMemo<CompletionContextType | undefined>(
        () =>
            monacoLanguage && SCRIPT_MONACO_LANGUAGES.includes(monacoLanguage)
                ? {components, kind: 'codeWorkflow', language: monacoLanguage as ScriptLanguageType}
                : undefined,
        [components, monacoLanguage]
    );

    useCompletionContext(model, completionContext);

    const handleSave = () => {
        onSave(latestSourceRef.current)
            .then(() => {
                setIsSourceDirty(false);
            })
            .catch(() => {});
    };

    const handleSourceChange = (value: string | undefined) => {
        setIsSourceDirty(true);
        latestSourceRef.current = value ?? '';
    };

    useEffect(() => {
        latestSourceRef.current = source ?? '';

        setIsSourceDirty(false);
    }, [source]);

    useEffect(() => {
        if (!headerless) {
            return;
        }

        setCodeWorkflowHeaderState({
            dirty: isSourceDirty,
            language,
            onSaveClick: handleSave,
            onTestConfigurationClick,
            saving: isSaving,
            testConfigurationDisabled: testConfigurationDisabled ?? true,
        });

        return () => reset();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [headerless, isSourceDirty, isSaving, language, onTestConfigurationClick, testConfigurationDisabled]);

    return (
        <LayoutContainer
            header={
                headerless ? undefined : (
                    <CodeWorkflowSourceEditorHeader
                        isSaveDisabled={!isSourceDirty || isSaving}
                        isSaving={isSaving}
                        language={language}
                        onSave={handleSave}
                        onTestConfigurationClick={onTestConfigurationClick}
                        testConfigurationDisabled={testConfigurationDisabled}
                    />
                )
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[error]} loading={isLoading}>
                {monacoLanguage && (
                    <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-lg bg-background">
                        <div className="relative min-h-0 flex-1">
                            <div className="absolute inset-0">
                                <MonacoEditorWrapper
                                    defaultLanguage={monacoLanguage}
                                    onChange={handleSourceChange}
                                    onMount={(mountedEditor) => setModel(mountedEditor.getModel() ?? undefined)}
                                    options={{
                                        automaticLayout: true,
                                        folding: true,
                                        lineNumbers: 'on',
                                        minimap: {enabled: false},
                                        scrollBeyondLastLine: false,
                                        wordWrap: 'on',
                                    }}
                                    value={isSourceDirty ? latestSourceRef.current : (source ?? '')}
                                />
                            </div>
                        </div>
                    </div>
                )}
            </PageLoader>
        </LayoutContainer>
    );
};

export default CodeWorkflowSourceEditor;
