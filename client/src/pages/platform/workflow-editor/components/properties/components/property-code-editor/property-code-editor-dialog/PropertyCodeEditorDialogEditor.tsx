import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
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

    return (
        <Suspense fallback={<MonacoEditorLoader />}>
            <MonacoEditor
                className="size-full"
                defaultLanguage={language}
                onChange={(value) => setEditorValue(value)}
                onMount={(editor) => {
                    editor.focus();
                }}
                value={editorValue}
            />
        </Suspense>
    );
};

export default PropertyCodeEditorDialogEditor;
