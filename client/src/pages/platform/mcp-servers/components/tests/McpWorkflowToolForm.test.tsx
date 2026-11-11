import {PropertyAllType} from '@/shared/types';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpWorkflowToolForm from '../McpWorkflowToolForm';

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: ({properties}: {properties: Array<{name: string}>}) => (
        <div data-testid="properties">{properties.map((property) => property.name).join(',')}</div>
    ),
}));

const properties = [
    {controlType: 'TEXT', name: 'toolName', type: 'STRING'},
    {controlType: 'TEXT_AREA', name: 'toolDescription', type: 'STRING'},
    {controlType: 'TEXT', name: 'channel', type: 'STRING'},
    {controlType: 'SELECT', name: 'level', optionsDataSource: {}, type: 'STRING'},
] as unknown as Array<PropertyAllType>;

let formRef: ReturnType<typeof useForm<Record<string, unknown>>> | undefined;

const Harness = ({defaultValues = {}}: {defaultValues?: Record<string, unknown>}) => {
    const form = useForm<Record<string, unknown>>({defaultValues});

    formRef = form;

    return <McpWorkflowToolForm form={form} properties={properties} />;
};

describe('McpWorkflowToolForm', () => {
    beforeEach(() => {
        formRef = undefined;
    });

    it('renders let ai fill all below tool description', async () => {
        render(<Harness />);

        const propertiesBlocks = screen.getAllByTestId('properties');

        expect(propertiesBlocks[0]).toHaveTextContent('toolName,toolDescription');

        const button = await screen.findByRole('button', {name: 'Let AI fill all'});

        expect(propertiesBlocks[0].compareDocumentPosition(button) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('fills only empty eligible fields on click and leaves dynamic options empty', async () => {
        render(<Harness />);

        const button = await screen.findByRole('button', {name: 'Let AI fill all'});

        await waitFor(() => expect(button).toBeEnabled());

        await userEvent.click(button);

        await waitFor(() => expect(formRef!.getValues('channel')).toMatch(/^=fromAi\(/));

        expect(formRef!.getValues('level')).toBeUndefined();
    });

    it('does not overwrite typed values', async () => {
        render(<Harness defaultValues={{channel: 'general'}} />);

        const button = await screen.findByRole('button', {name: 'Let AI fill all'});

        await waitFor(() => expect(button).toBeDisabled());
    });
});
