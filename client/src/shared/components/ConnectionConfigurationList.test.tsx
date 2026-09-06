import {Form} from '@/components/ui/form';
import {ConnectionI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import ConnectionConfigurationList from '@/shared/components/ConnectionConfigurationList';
import {ComponentConnection, Workflow} from '@/shared/middleware/platform/configuration';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {Control, FieldValues, useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: {name: 'httpClient', title: 'HTTP Client'}}),
}));

const optionalComponentConnection: ComponentConnection = {
    componentName: 'httpClient',
    componentVersion: 1,
    key: 'httpClient_1',
    required: false,
    workflowNodeName: 'httpClient_1',
};

const workflow = {
    id: 'workflow-1',
    tasks: [{name: 'httpClient_1', type: 'httpClient/v1/get'}],
} as Workflow;

const connection: ConnectionI = {
    componentName: 'httpClient',
    connectionVersion: 1,
    environmentId: 1,
    id: 42,
    name: 'My HTTP Client connection',
    parameters: {},
};

interface ConnectionConfigurationListHarnessProps {
    connections: ConnectionI[];
    handleConnectionIdChange: (index: number, connectionId: number | undefined) => void;
}

const ConnectionConfigurationListHarness = ({
    connections,
    handleConnectionIdChange,
}: ConnectionConfigurationListHarnessProps) => {
    const form = useForm<FieldValues>({defaultValues: {connections: [{}]}});

    return (
        <Form {...form}>
            <ConnectionConfigurationList
                componentConnections={[optionalComponentConnection]}
                connections={connections}
                control={form.control as Control<FieldValues>}
                handleConnectionIdChange={handleConnectionIdChange}
                workflow={workflow}
            />
        </Form>
    );
};

describe('ConnectionConfigurationList', () => {
    it('should report a cleared connection as undefined rather than NaN', () => {
        const handleConnectionIdChange = vi.fn();

        render(
            <ConnectionConfigurationListHarness
                connections={[connection]}
                handleConnectionIdChange={handleConnectionIdChange}
            />
        );

        fireEvent.click(screen.getByRole('combobox'));

        fireEvent.click(screen.getByRole('option', {name: 'Select a connection...'}));

        expect(handleConnectionIdChange).toHaveBeenCalledWith(0, undefined);
    });

    it('should report a selected connection as its numeric id', () => {
        const handleConnectionIdChange = vi.fn();

        render(
            <ConnectionConfigurationListHarness
                connections={[connection]}
                handleConnectionIdChange={handleConnectionIdChange}
            />
        );

        fireEvent.click(screen.getByRole('combobox'));

        fireEvent.click(screen.getByRole('option', {name: /My HTTP Client connection/}));

        expect(handleConnectionIdChange).toHaveBeenCalledWith(0, 42);
    });

    it('should offer the cleared connection option even when no connection exists for the component', () => {
        const handleConnectionIdChange = vi.fn();

        render(
            <ConnectionConfigurationListHarness connections={[]} handleConnectionIdChange={handleConnectionIdChange} />
        );

        fireEvent.click(screen.getByRole('combobox'));

        fireEvent.click(screen.getByRole('option', {name: 'Select a connection...'}));

        expect(handleConnectionIdChange).toHaveBeenCalledWith(0, undefined);
    });
});
