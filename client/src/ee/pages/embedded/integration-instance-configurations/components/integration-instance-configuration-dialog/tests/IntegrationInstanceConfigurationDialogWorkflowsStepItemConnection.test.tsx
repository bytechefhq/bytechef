import {Form} from '@/components/ui/form';
import {IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {ComponentConnection} from '@/shared/middleware/platform/configuration';
import {render, screen} from '@/shared/util/test-utils';
import {useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection from '../IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection';

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: {name: 'googleSheets', title: 'Google Sheets'}}),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetConnectionTagsQuery: vi.fn(),
    useGetConnectionsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/mutations/embedded/connections.mutations', () => ({
    useCreateConnectionMutation: vi.fn(),
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: () => null,
}));

const optionalComponentConnection: ComponentConnection = {
    componentName: 'googleSheets',
    componentVersion: 1,
    key: 'googleSheets_1',
    required: false,
    workflowNodeName: 'googleSheets_1',
};

const WorkflowsStepItemConnectionHarness = ({componentConnection}: {componentConnection: ComponentConnection}) => {
    const form = useForm<IntegrationInstanceConfiguration>();

    return (
        <Form {...form}>
            <IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection
                componentConnection={componentConnection}
                componentConnectionIndex={0}
                control={form.control}
                workflowIndex={0}
            />
        </Form>
    );
};

describe('IntegrationInstanceConfigurationDialogWorkflowsStepItemConnection', () => {
    it('should mark a required connection with an asterisk', () => {
        render(
            <WorkflowsStepItemConnectionHarness
                componentConnection={{...optionalComponentConnection, required: true}}
            />
        );

        expect(screen.getByText('Google Sheets Connection')).toHaveTextContent(/^Google Sheets Connection\*$/);
    });

    it('should not mark an optional connection with an asterisk', () => {
        render(<WorkflowsStepItemConnectionHarness componentConnection={optionalComponentConnection} />);

        expect(screen.getByText('Google Sheets Connection')).toHaveTextContent(/^Google Sheets Connection$/);
    });
});
