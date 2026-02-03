import LoadingDots from '@/components/LoadingDots';
import {usePropertyCodeEditorDialogStore} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/stores/usePropertyCodeEditorDialogStore';
import {RefreshCwIcon} from 'lucide-react';
import {Suspense, lazy} from 'react';
import {useShallow} from 'zustand/react/shallow';

const ReactJson = lazy(() => import('react-json-view'));

const PropertyCodeEditorDialogExecutionOutput = () => {
    const {scriptIsRunning, scriptTestExecution} = usePropertyCodeEditorDialogStore(
        useShallow((state) => ({
            scriptIsRunning: state.scriptIsRunning,
            scriptTestExecution: state.scriptTestExecution,
        }))
    );

    return (
        <div className="relative size-full overflow-y-auto p-4 text-sm">
            {scriptIsRunning ? (
                <div className="flex items-center gap-x-1">
                    <span className="flex animate-spin text-gray-400">
                        <RefreshCwIcon className="size-4" />
                    </span>

                    <span className="text-muted-foreground">Script is running...</span>
                </div>
            ) : !scriptTestExecution ? (
                <span className="text-muted-foreground">The script has not yet been executed.</span>
            ) : scriptTestExecution.output ? (
                typeof scriptTestExecution.output === 'object' ? (
                    <Suspense fallback={<LoadingDots />}>
                        <ReactJson enableClipboard={false} sortKeys={true} src={scriptTestExecution.output as object} />
                    </Suspense>
                ) : (
                    <pre className="mt-2 text-xs">{scriptTestExecution.output}</pre>
                )
            ) : scriptTestExecution.error ? (
                <div>
                    <span className="font-semibold text-destructive">Error</span>

                    <p>{scriptTestExecution.error.message}</p>
                </div>
            ) : (
                <span className="text-muted-foreground">No defined output.</span>
            )}
        </div>
    );
};

export default PropertyCodeEditorDialogExecutionOutput;
