import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import {useWorkflowEditorReadOnly} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {Suspense, lazy} from 'react';
import {useShallow} from 'zustand/react/shallow';

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

interface PropertyCodeEditorDialogEditorProps {
    language: string;
}

const PropertyCodeEditorDialogEditor = ({language}: PropertyCodeEditorDialogEditorProps) => {
    const {editorValue, setEditorValue} = usePropertyCodeEditorDialogStore(
        useShallow((state) => ({
            editorValue: state.editorValue,
            setEditorValue: state.setEditorValue,
        }))
    );

    const readOnly = useWorkflowEditorReadOnly();

    return (
        <Suspense fallback={<MonacoEditorLoader />}>
            <MonacoEditor
                className="size-full"
                defaultLanguage={language}
                onChange={(value) => {
                    if (!readOnly) {
                        setEditorValue(value);
                    }
                }}
                onMount={(editor) => {
                    editor.focus();
                }}
                options={{readOnly}}
                value={editorValue}
            />
        </Suspense>
    );
};

export default PropertyCodeEditorDialogEditor;
