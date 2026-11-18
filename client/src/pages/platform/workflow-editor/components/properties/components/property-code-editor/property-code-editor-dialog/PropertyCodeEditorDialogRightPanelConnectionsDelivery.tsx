import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {TriangleAlertIcon} from 'lucide-react';
import {useMemo} from 'react';

const GRAALVM_TASK_RUNNER_TYPE = 'graalvm';

interface PropertyCodeEditorDialogRightPanelConnectionsDeliveryProps {
    language: string;
}

const getTaskRunnerType = (parameters: Record<string, unknown> | undefined): string | undefined => {
    const taskRunner = parameters?.taskRunner;

    if (!taskRunner || typeof taskRunner !== 'object') {
        return undefined;
    }

    const {type} = taskRunner as {type?: unknown};

    return typeof type === 'string' ? type : undefined;
};

const PropertyCodeEditorDialogRightPanelConnectionsDelivery = ({
    language,
}: PropertyCodeEditorDialogRightPanelConnectionsDeliveryProps) => {
    const injectConnections = useApplicationInfoStore((state) => state.script.injectConnections);
    const currentNodeParameters = useWorkflowNodeDetailsPanelStore((state) => state.currentNode?.parameters);

    const shellScript = language.toLowerCase() === 'shell';

    const externalTaskRunner = useMemo(() => {
        if (shellScript) {
            return true;
        }

        const taskRunnerType = getTaskRunnerType(currentNodeParameters);

        return taskRunnerType !== undefined && taskRunnerType !== GRAALVM_TASK_RUNNER_TYPE;
    }, [currentNodeParameters, shellScript]);

    if (!externalTaskRunner) {
        return null;
    }

    if (!injectConnections) {
        return (
            <div
                className="mb-3 flex gap-2 rounded-md bg-surface-warning-secondary p-3 text-sm text-content-neutral-primary"
                role="alert"
            >
                <TriangleAlertIcon aria-hidden="true" className="mt-0.5 size-4 shrink-0" />

                <p>
                    This server does not pass connections to external task runners, so the script will not receive them.
                    {shellScript
                        ? 'Turn on bytechef.script.inject-connections.'
                        : 'Turn on bytechef.script.inject-connections or run the script in the GraalVM task runner.'}
                </p>
            </div>
        );
    }

    return (
        <p className="mb-3 text-sm text-content-neutral-secondary">
            {shellScript ? (
                <>
                    The script reads these connections from the JSON file at <code>$BYTECHEF_CONNECTIONS_FILE</code>,
                    keyed by connection name. Type <code>connection.</code> in the editor to insert a read.
                </>
            ) : (
                <>
                    The task runner passes these connections to <code>perform</code> as its second argument, keyed by
                    connection name: <code>perform(input, connection, context)</code>.
                </>
            )}
        </p>
    );
};

export default PropertyCodeEditorDialogRightPanelConnectionsDelivery;
