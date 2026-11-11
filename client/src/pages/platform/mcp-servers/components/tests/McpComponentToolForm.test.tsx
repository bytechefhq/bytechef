import {PropertyAllType} from '@/shared/types';
import {act, render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentToolForm from '../McpComponentToolForm';

const hoisted = vi.hoisted(() => ({
    displayConditions: undefined as Record<string, boolean> | undefined,
    isEvaluating: false,
    loadOptions: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: ({properties}: {properties: Array<{name: string}>}) => (
        <div data-testid="properties">{properties.map((property) => property.name).join(',')}</div>
    ),
}));

vi.mock('@/shared/queries/platform/useFormDisplayConditions', () => ({
    default: () => ({displayConditions: hoisted.displayConditions, isEvaluating: hoisted.isEvaluating}),
}));

vi.mock('@/pages/platform/mcp-servers/hooks/useMcpToolOptionsLoader', () => ({
    default: () => hoisted.loadOptions,
}));

const properties = [
    {controlType: 'TEXT', name: 'toolName', type: 'STRING'},
    {controlType: 'TEXT_AREA', name: 'toolDescription', type: 'STRING'},
    {controlType: 'TEXT', name: 'uri', type: 'STRING'},
    {controlType: 'TEXT', name: 'subject', type: 'STRING'},
] as Array<PropertyAllType>;

let formRef: ReturnType<typeof useForm<Record<string, unknown>>> | undefined;

const Harness = ({
    defaultValues = {},
    toolProperties = properties,
}: {
    defaultValues?: Record<string, unknown>;
    toolProperties?: Array<PropertyAllType>;
}) => {
    const form = useForm<Record<string, unknown>>({defaultValues});

    formRef = form;

    return (
        <McpComponentToolForm
            componentName="httpClient"
            componentVersion={1}
            form={form}
            properties={toolProperties}
            toolName="post"
        />
    );
};

describe('McpComponentToolForm', () => {
    beforeEach(() => {
        formRef = undefined;
        hoisted.displayConditions = undefined;
        hoisted.isEvaluating = false;
        hoisted.loadOptions.mockReset();
        hoisted.loadOptions.mockResolvedValue(null);
    });

    it('renders let ai fill all below tool description', async () => {
        render(<Harness />);

        const propertiesBlocks = screen.getAllByTestId('properties');

        expect(propertiesBlocks[0]).toHaveTextContent('toolName,toolDescription');
        expect(propertiesBlocks[1]).toHaveTextContent('uri,subject');

        const button = await screen.findByRole('button', {name: 'Let AI fill all'});

        expect(propertiesBlocks[0].compareDocumentPosition(button) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
        expect(propertiesBlocks[1].compareDocumentPosition(button) & Node.DOCUMENT_POSITION_PRECEDING).toBeTruthy();
    });

    it('fills only empty eligible fields on click', async () => {
        render(<Harness defaultValues={{uri: 'https://example.com'}} />);

        const button = await screen.findByRole('button', {name: 'Let AI fill all'});

        await waitFor(() => expect(button).toBeEnabled());

        await userEvent.click(button);

        await waitFor(() => expect(formRef!.getValues('subject')).toMatch(/^=fromAi\(/));

        expect(formRef!.getValues('uri')).toBe('https://example.com');
        expect(formRef!.getValues('toolName')).toBeUndefined();
        expect(formRef!.formState.dirtyFields.subject).toBe(true);
    });

    it('does not overwrite typed values', async () => {
        render(<Harness />);

        const button = await screen.findByRole('button', {name: 'Let AI fill all'});

        await waitFor(() => expect(button).toBeEnabled());

        act(() => formRef!.setValue('subject', 'typed', {shouldDirty: true}));

        await userEvent.click(button);

        await waitFor(() => expect(formRef!.getValues('uri')).toMatch(/^=fromAi\(/));

        expect(formRef!.getValues('subject')).toBe('typed');
    });

    it('disables let ai fill all when nothing is eligible', async () => {
        render(<Harness defaultValues={{subject: 'a', uri: 'b'}} />);

        const button = await screen.findByRole('button', {name: 'Let AI fill all'});

        await waitFor(() => expect(button).toBeDisabled());
    });

    it('skips properties hidden by display conditions', async () => {
        hoisted.displayConditions = {"mode == 'a'": false};

        render(
            <Harness
                toolProperties={
                    [
                        {controlType: 'TEXT', displayCondition: "mode == 'a'", name: 'uri', type: 'STRING'},
                        {controlType: 'TEXT', name: 'subject', type: 'STRING'},
                    ] as Array<PropertyAllType>
                }
            />
        );

        const button = await screen.findByRole('button', {name: 'Let AI fill all'});

        await waitFor(() => expect(button).toBeEnabled());

        await userEvent.click(button);

        await waitFor(() => expect(formRef!.getValues('subject')).toMatch(/^=fromAi\(/));

        expect(formRef!.getValues('uri')).toBeUndefined();
    });

    it('makes no options request while typing and loads options only on click', async () => {
        hoisted.loadOptions.mockResolvedValue([{value: 'general'}]);

        render(
            <Harness
                toolProperties={
                    [
                        {controlType: 'TEXT', name: 'toolName', type: 'STRING'},
                        {controlType: 'SELECT', name: 'channel', optionsDataSource: {}, type: 'STRING'},
                    ] as Array<PropertyAllType>
                }
            />
        );

        const button = await screen.findByRole('button', {name: 'Let AI fill all'});

        await waitFor(() => expect(button).toBeEnabled());

        act(() => formRef!.setValue('toolName', 'my tool', {shouldDirty: true}));
        act(() => formRef!.setValue('toolName', 'my tool two', {shouldDirty: true}));

        await waitFor(() => expect(button).toBeEnabled());

        expect(hoisted.loadOptions).not.toHaveBeenCalled();

        await userEvent.click(button);

        await waitFor(() => expect(formRef!.getValues('channel')).toMatch(/^=fromAi\('channel'/));

        expect(hoisted.loadOptions).toHaveBeenCalledTimes(1);
    });
});
