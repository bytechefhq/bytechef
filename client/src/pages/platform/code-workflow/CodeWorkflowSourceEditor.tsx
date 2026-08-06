import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import PageLoader from '@/components/PageLoader';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import MonacoEditorWrapper from '@/shared/components/MonacoEditorWrapper';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {CodeWorkflowLanguage} from '@/shared/middleware/graphql';
import {Settings2Icon} from 'lucide-react';
import {useEffect, useRef, useState} from 'react';

const MONACO_LANGUAGE_BY_CODE_WORKFLOW_LANGUAGE: Record<CodeWorkflowLanguage, string> = {
    [CodeWorkflowLanguage.Javascript]: 'javascript',
    [CodeWorkflowLanguage.Python]: 'python',
    [CodeWorkflowLanguage.Ruby]: 'ruby',
};

export interface CodeWorkflowSourceEditorProps {
    error?: unknown;
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
            <div className="flex items-center gap-2">
                {onTestConfigurationClick && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span tabIndex={0}>
                                <Button
                                    disabled={testConfigurationDisabled}
                                    icon={<Settings2Icon />}
                                    label="Test Configuration"
                                    onClick={onTestConfigurationClick}
                                    size="sm"
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

                <Badge label={language} styleType="secondary-filled" weight="semibold" />
            </div>
        }
    />
);

const CodeWorkflowSourceEditor = ({
    error,
    isLoading,
    isSaving,
    language,
    onSave,
    onTestConfigurationClick,
    source,
    testConfigurationDisabled,
}: CodeWorkflowSourceEditorProps) => {
    const [isSourceDirty, setIsSourceDirty] = useState(false);

    const latestSourceRef = useRef(source ?? '');

    const monacoLanguage = MONACO_LANGUAGE_BY_CODE_WORKFLOW_LANGUAGE[language as CodeWorkflowLanguage];

    const handleSave = () => {
        onSave(latestSourceRef.current)
            .then(() => {
                setIsSourceDirty(false);
            })
            .catch(() => {
                // Error is already surfaced via the global fetch interceptor toast; stay dirty.
            });
    };

    const handleSourceChange = (value: string | undefined) => {
        setIsSourceDirty(true);
        latestSourceRef.current = value ?? '';
    };

    useEffect(() => {
        latestSourceRef.current = source ?? '';

        setIsSourceDirty(false);
    }, [source]);

    return (
        <LayoutContainer
            header={
                <CodeWorkflowSourceEditorHeader
                    isSaveDisabled={!isSourceDirty || isSaving}
                    isSaving={isSaving}
                    language={language}
                    onSave={handleSave}
                    onTestConfigurationClick={onTestConfigurationClick}
                    testConfigurationDisabled={testConfigurationDisabled}
                />
            }
            leftSidebarOpen={false}
        >
            <PageLoader errors={[error]} loading={isLoading}>
                {monacoLanguage && (
                    <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
                        <div className="relative min-h-0 flex-1">
                            <div className="absolute inset-0">
                                <MonacoEditorWrapper
                                    defaultLanguage={monacoLanguage}
                                    onChange={handleSourceChange}
                                    onMount={() => {}}
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
