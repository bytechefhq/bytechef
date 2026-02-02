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

    const renderContent = () => {
        if (scriptIsRunning) {
            return (
                <div className="flex items-center gap-x-1">
                    <span className="flex animate-spin text-gray-400">
                        <RefreshCwIcon className="size-4" />
                    </span>

                    <span className="text-muted-foreground">Script is running...</span>
                </div>
            );
        }

        if (!scriptTestExecution) {
            return (
                <div className="flex items-center gap-x-1 text-muted-foreground">
                    <span>The script has not yet been executed.</span>
                </div>
            );
        }

        if (scriptTestExecution.output) {
            if (typeof scriptTestExecution.output === 'object') {
                return (
                    <Suspense fallback={<LoadingDots />}>
                        <ReactJson enableClipboard={false} sortKeys={true} src={scriptTestExecution.output as object} />
                    </Suspense>
                );
            }

            return <pre className="mt-2 text-xs">{scriptTestExecution.output}</pre>;
        }

        if (scriptTestExecution.error) {
            return (
                <div className="space-y-4">
                    <div className="space-y-2">
                        <div className="font-semibold text-destructive">Error</div>

                        <div>{scriptTestExecution.error.message}</div>
                    </div>
                </div>
            );
        }

        return <span className="text-muted-foreground">No defined output.</span>;
    };

    return <div className="relative size-full overflow-y-auto p-4 text-sm">{renderContent()}</div>;
};

export default PropertyCodeEditorDialogExecutionOutput;
