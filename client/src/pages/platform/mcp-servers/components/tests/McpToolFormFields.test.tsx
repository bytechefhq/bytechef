import {PropertyAllType} from '@/shared/types';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpToolFormFields from '../McpToolFormFields';

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: ({
        onAdvancedPropertiesOpenChange,
        properties,
    }: {
        onAdvancedPropertiesOpenChange?: (open: boolean) => void;
        properties: Array<{name: string}>;
    }) => (
        <div data-testid="properties">
            {properties.map((property) => property.name).join(',')}

            {onAdvancedPropertiesOpenChange && (
                <button onClick={() => onAdvancedPropertiesOpenChange(true)} type="button">
                    Show Advanced Properties
                </button>
            )}
        </div>
    ),
}));

const properties = [
    {controlType: 'TEXT', name: 'toolName', type: 'STRING'},
    {controlType: 'TEXT_AREA', name: 'toolDescription', type: 'STRING'},
    {controlType: 'TEXT', name: 'subject', type: 'STRING'},
    {advancedOption: true, controlType: 'TEXT', name: 'replyTo', type: 'STRING'},
] as Array<PropertyAllType>;

let formRef: ReturnType<typeof useForm<Record<string, unknown>>> | undefined;

const Harness = () => {
    const form = useForm<Record<string, unknown>>({defaultValues: {}});

    formRef = form;

    return <McpToolFormFields form={form} properties={properties} values={form.watch()} />;
};

describe('McpToolFormFields', () => {
    beforeEach(() => {
        formRef = undefined;
    });

    it('leaves advanced fields empty while the advanced properties are collapsed', async () => {
        render(<Harness />);

        await userEvent.click(await screen.findByRole('button', {name: 'Let AI fill all'}));

        await waitFor(() => expect(formRef!.getValues('subject')).toMatch(/^=fromAi\(/));

        expect(formRef!.getValues('replyTo')).toBeUndefined();
    });

    it('fills advanced fields once the advanced properties are expanded', async () => {
        render(<Harness />);

        await userEvent.click(screen.getByRole('button', {name: 'Show Advanced Properties'}));

        await userEvent.click(await screen.findByRole('button', {name: 'Let AI fill all'}));

        await waitFor(() => expect(formRef!.getValues('replyTo')).toMatch(/^=fromAi\(/));

        expect(formRef!.getValues('subject')).toMatch(/^=fromAi\(/);
    });

    it('enables let ai fill all when only an advanced field is empty and the advanced properties are expanded', async () => {
        const Prefilled = () => {
            const form = useForm<Record<string, unknown>>({defaultValues: {subject: 'Hello'}});

            formRef = form;

            return <McpToolFormFields form={form} properties={properties} values={form.watch()} />;
        };

        render(<Prefilled />);

        const button = await screen.findByRole('button', {name: 'Let AI fill all'});

        await waitFor(() => expect(button).toBeDisabled());

        await userEvent.click(screen.getByRole('button', {name: 'Show Advanced Properties'}));

        await waitFor(() => expect(button).toBeEnabled());
    });

    it('keeps a value typed while the options are loading', async () => {
        let resolveOptions: (options: Array<{value?: unknown}>) => void = () => {};

        const loadOptions = vi.fn(
            () =>
                new Promise<Array<{value?: unknown}>>((resolve) => {
                    resolveOptions = resolve;
                })
        );

        const selectProperties = [
            {controlType: 'SELECT', name: 'priority', optionsDataSource: {}, type: 'STRING'},
            {controlType: 'TEXT', name: 'subject', type: 'STRING'},
        ] as Array<PropertyAllType>;

        const WithOptions = () => {
            const form = useForm<Record<string, unknown>>({defaultValues: {}});

            formRef = form;

            return (
                <McpToolFormFields
                    form={form}
                    loadOptions={loadOptions}
                    properties={selectProperties}
                    values={form.watch()}
                />
            );
        };

        render(<WithOptions />);

        await userEvent.click(await screen.findByRole('button', {name: 'Let AI fill all'}));

        await waitFor(() => expect(loadOptions).toHaveBeenCalled());

        formRef!.setValue('subject', 'Typed meanwhile');

        resolveOptions([{value: 'high'}]);

        await waitFor(() => expect(formRef!.getValues('priority')).toMatch(/^=fromAi\(/));

        expect(formRef!.getValues('subject')).toBe('Typed meanwhile');
    });

    it('disables let ai fill all when the options of the only empty field do not load', async () => {
        const loadOptions = vi.fn(async () => null);

        const selectProperties = [
            {controlType: 'SELECT', name: 'priority', optionsDataSource: {}, type: 'STRING'},
        ] as Array<PropertyAllType>;

        const OnlyOptions = () => {
            const form = useForm<Record<string, unknown>>({defaultValues: {}});

            return (
                <McpToolFormFields
                    form={form}
                    loadOptions={loadOptions}
                    properties={selectProperties}
                    values={form.watch()}
                />
            );
        };

        render(<OnlyOptions />);

        await waitFor(() => expect(loadOptions).toHaveBeenCalled());

        expect(screen.getByRole('button', {name: 'Let AI fill all'})).toBeDisabled();
    });

    it('enables let ai fill all once the options of the only empty field load', async () => {
        const loadOptions = vi.fn(async () => [{value: 'high'}, {value: 'low'}]);

        const selectProperties = [
            {controlType: 'SELECT', name: 'priority', optionsDataSource: {}, type: 'STRING'},
        ] as Array<PropertyAllType>;

        const OnlyOptions = () => {
            const form = useForm<Record<string, unknown>>({defaultValues: {}});

            formRef = form;

            return (
                <McpToolFormFields
                    form={form}
                    loadOptions={loadOptions}
                    properties={selectProperties}
                    values={form.watch()}
                />
            );
        };

        render(<OnlyOptions />);

        const button = screen.getByRole('button', {name: 'Let AI fill all'});

        await waitFor(() => expect(button).toBeEnabled());

        await userEvent.click(button);

        await waitFor(() => expect(formRef!.getValues('priority')).toMatch(/^=fromAi\(/));
    });
});
