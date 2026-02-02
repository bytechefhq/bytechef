import MonacoEditorWrapper from '@/shared/components/MonacoEditorWrapper';

interface EndpointYamlEditorProps {
    onChange: (value: string) => void;
    value: string;
}

const EndpointYamlEditor = ({onChange, value}: EndpointYamlEditorProps) => {
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
                onMount={() => {}}
                options={editorOptions}
                value={value}
            />
        </div>
    );
};

export default EndpointYamlEditor;
