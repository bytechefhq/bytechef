import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, screen} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import PropertyCodeEditorDialogRightPanelConnectionsDelivery from '../PropertyCodeEditorDialogRightPanelConnectionsDelivery';

const setTaskRunnerType = (taskRunnerType?: string) => {
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {
            componentName: 'script',
            label: 'Script',
            name: 'script_1',
            parameters: taskRunnerType ? {taskRunner: {type: taskRunnerType}} : {},
            workflowNodeName: 'script_1',
        },
    } as unknown as Parameters<typeof useWorkflowNodeDetailsPanelStore.setState>[0]);
};

const setInjectConnections = (injectConnections: boolean) => {
    applicationInfoStore.setState({script: {injectConnections}});
};

describe('PropertyCodeEditorDialogRightPanelConnectionsDelivery', () => {
    beforeEach(() => {
        setInjectConnections(true);
        setTaskRunnerType();
    });

    afterEach(() => {
        setInjectConnections(true);
        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined});
    });

    it('points a shell script at the connections file', () => {
        render(<PropertyCodeEditorDialogRightPanelConnectionsDelivery language="shell" />);

        expect(screen.getByText('$BYTECHEF_CONNECTIONS_FILE')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('tells a JavaScript script in an external runner that perform receives the connections', () => {
        setTaskRunnerType('docker');

        render(<PropertyCodeEditorDialogRightPanelConnectionsDelivery language="javascript" />);

        expect(screen.getByText('perform(input, connection, context)')).toBeInTheDocument();
    });

    it('shows nothing for a script in the GraalVM task runner', () => {
        setTaskRunnerType('graalvm');

        const {container} = render(<PropertyCodeEditorDialogRightPanelConnectionsDelivery language="python" />);

        expect(container).toBeEmptyDOMElement();
    });

    it('shows nothing for a script that leaves the task runner to its default', () => {
        const {container} = render(<PropertyCodeEditorDialogRightPanelConnectionsDelivery language="javascript" />);

        expect(container).toBeEmptyDOMElement();
    });

    it('warns that a shell script gets no connections when the server does not inject them', () => {
        setInjectConnections(false);

        render(<PropertyCodeEditorDialogRightPanelConnectionsDelivery language="shell" />);

        const alert = screen.getByRole('alert');

        expect(alert).toHaveTextContent('bytechef.script.inject-connections');
        expect(alert).not.toHaveTextContent('GraalVM');
    });

    it('offers the GraalVM task runner as the way out for a JavaScript script', () => {
        setInjectConnections(false);
        setTaskRunnerType('process');

        render(<PropertyCodeEditorDialogRightPanelConnectionsDelivery language="javascript" />);

        expect(screen.getByRole('alert')).toHaveTextContent('GraalVM task runner');
    });
});
