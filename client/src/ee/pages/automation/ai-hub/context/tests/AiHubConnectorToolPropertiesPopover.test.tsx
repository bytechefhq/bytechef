import AiHubConnectorToolPropertiesPopover from '@/ee/pages/automation/ai-hub/context/AiHubConnectorToolPropertiesPopover';
import {useClusterElementContext} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import {PropertyAllType} from '@/shared/types';
import {render, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/components/ui/popover', () => ({
    PopoverContent: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowReadOnlyProvider: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/ee/pages/automation/ai-hub/context/hooks/useAiHubConnectorToolPropertiesPopover', async () => {
    const {useForm} = await import('react-hook-form');

    const useAiHubConnectorToolPropertiesPopover = () => {
        const form = useForm({defaultValues: {baseId: ''}});

        return {
            control: form.control,
            form,
            formState: form.formState,
            handleFormSubmit: vi.fn(),
            handleSubmit: form.handleSubmit,
            isLoading: false,
            properties: [{controlType: 'TEXT', name: 'baseId', type: 'STRING'}] as Array<PropertyAllType>,
        };
    };

    return {default: useAiHubConnectorToolPropertiesPopover};
});

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => {
    const PropertiesStub = () => {
        const clusterElementContext = useClusterElementContext();

        return (
            <div
                data-connection-id={String(clusterElementContext?.connectionId)}
                data-connection-required={String(clusterElementContext?.connectionRequired)}
                data-testid="properties"
            />
        );
    };

    return {default: PropertiesStub};
});

const renderPopover = (connectionRequired?: boolean) =>
    render(
        <AiHubConnectorToolPropertiesPopover
            componentName="airtable"
            componentVersion={1}
            connectionRequired={connectionRequired}
            connectorId="7"
            onClose={vi.fn()}
            tool={{name: 'search', title: 'Search'}}
            workspaceId="1"
        />
    );

describe('AiHubConnectorToolPropertiesPopover', () => {
    it('carries connectionRequired into the cluster element context when no connection is bound', () => {
        renderPopover(true);

        const properties = screen.getByTestId('properties');

        expect(properties).toHaveAttribute('data-connection-required', 'true');
        expect(properties).toHaveAttribute('data-connection-id', 'undefined');
    });

    it('leaves connectionRequired undefined when the caller does not supply it', () => {
        renderPopover();

        expect(screen.getByTestId('properties')).toHaveAttribute('data-connection-required', 'undefined');
    });
});
