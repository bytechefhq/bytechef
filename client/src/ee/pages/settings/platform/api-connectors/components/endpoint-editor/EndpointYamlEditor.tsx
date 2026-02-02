import MonacoEditorWrapper from '@/shared/components/MonacoEditorWrapper';
import {StandaloneCodeEditorType} from '@/shared/components/MonacoTypes';
import {useRef} from 'react';

interface EndpointYamlEditorProps {
    onChange: (value: string) => void;
    value: string;
}

const EndpointYamlEditor = ({onChange, value}: EndpointYamlEditorProps) => {
    const editorRef = useRef<StandaloneCodeEditorType | null>(null);

    const editorOptions = {
        automaticLayout: true,
        folding: true,
        fontSize: 12,
        lineNumbers: 'on' as const,
        minimap: {enabled: false},
        scrollBeyondLastLine: false,
        tabSize: 2,
        wordWrap: 'on' as const,
    };

    return (
        <div className="h-80 overflow-hidden rounded-md border">
            <MonacoEditorWrapper
                defaultLanguage="yaml"
                onChange={(newValue) => onChange(newValue || '')}
                onMount={(editor) => {
                    editorRef.current = editor;
                }}
                options={editorOptions}
                value={value}
            />
        </div>
    );
};

export default EndpointYamlEditor;
