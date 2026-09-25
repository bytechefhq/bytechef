vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {TooltipProvider} from '@/components/ui/tooltip';
import {FormulaEnabledProvider} from '@/pages/platform/workflow-editor/components/properties/FormulaEnabledContext';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {fireEvent, screen, waitFor, within} from '@testing-library/react';
import {ReactNode} from 'react';
import {FormProvider, useForm} from 'react-hook-form';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

let formValues: Record<string, unknown> = {};

const Wrapper = ({
    formulaEnabled,
    property,
    toolsMode = true,
    value,
}: {
    formulaEnabled?: boolean;
    property: PropertyAllType;
    toolsMode?: boolean;
    value: unknown;
}) => {
    const form = useForm({defaultValues: {[property.name!]: value}});

    formValues = form.watch();

    let content: ReactNode = (
        <Property
            control={form.control as never}
            controlPath=""
            formState={form.formState}
            property={property}
            toolsMode={toolsMode}
        />
    );

    if (formulaEnabled !== undefined) {
        content = <FormulaEnabledProvider value={formulaEnabled}>{content}</FormulaEnabledProvider>;
    }

    return (
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>{content}</WorkflowEditorProvider>
        </TooltipProvider>
    );
};

const ArrayWrapper = ({property, value}: {property: PropertyAllType; value: unknown[]}) => {
    const form = useForm({defaultValues: {[property.name!]: value}});

    formValues = form.watch();

    return (
        <FormProvider {...form}>
            <TooltipProvider>
                <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                    <Property
                        control={form.control as never}
                        controlPath=""
                        formState={form.formState}
                        property={property}
                        toolsMode
                    />
                </WorkflowEditorProvider>
            </TooltipProvider>
        </FormProvider>
    );
};

const microtaskTick = async (times = 1) => {
    for (let index = 0; index < times; index++) {
        await new Promise<void>((resolve) => setTimeout(resolve, 0));
        await Promise.resolve();
    }
};

const settle = async () => {
    await microtaskTick(4);
    await new Promise<void>((resolve) => setTimeout(resolve, 120));
    await microtaskTick(2);
};

const countProperty = {
    controlType: 'INTEGER',
    expressionEnabled: true,
    label: 'Count',
    name: 'count',
    type: 'INTEGER',
} as PropertyAllType;

const uriProperty = {
    controlType: 'TEXT',
    expressionEnabled: true,
    label: 'URI',
    name: 'uri',
    type: 'STRING',
} as PropertyAllType;

const enabledProperty = {
    controlType: 'SELECT',
    expressionEnabled: true,
    label: 'Enabled',
    name: 'enabled',
    type: 'BOOLEAN',
} as PropertyAllType;

const prioritySelectProperty = {
    controlType: 'SELECT',
    expressionEnabled: true,
    label: 'Priority',
    name: 'priority',
    options: [
        {label: 'One', value: '1'},
        {label: 'Two', value: '2'},
    ],
    type: 'INTEGER',
} as PropertyAllType;

describe('controlled Formula switch', () => {
    afterEach(() => {
        vi.restoreAllMocks();
    });

    beforeEach(() => {
        formValues = {};

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-controlled-formula', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: undefined,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('switches a controlled array item saved as a formula back to its native control', async () => {
        const countsProperty = {
            controlType: 'ARRAY_BUILDER',
            expressionEnabled: true,
            items: [countProperty],
            label: 'Counts',
            name: 'counts',
            type: 'ARRAY',
        } as PropertyAllType;

        const {container} = render(<ArrayWrapper property={countsProperty} value={['=1+1']} />);

        await settle();

        const itemElement = screen.getByLabelText('0 property');

        expect(itemElement.querySelector('.ProseMirror')).not.toBeNull();

        fireEvent.click(within(itemElement).getByRole('switch', {name: 'Formula'}));

        await waitFor(() => expect((formValues.counts as unknown[])[0]).toBe(''));

        await settle();

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(itemElement.querySelector('input[type=number]')).not.toBeNull();
        expect(within(itemElement).getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'false');
    });

    it('converts a constant number into a formula', async () => {
        const {container} = render(<Wrapper property={countProperty} value={5} />);

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await waitFor(() => expect(formValues.count).toBe('=5'));

        expect(container.querySelector('.ProseMirror')).not.toBeNull();
    });

    it('converts a literal formula back into a number', async () => {
        render(<Wrapper property={countProperty} value="=7" />);

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await waitFor(() => expect(formValues.count).toBe(7));
    });

    it('clears a formula that cannot convert back', async () => {
        render(<Wrapper property={countProperty} value="=concat(a, b)" />);

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await waitFor(() => expect(formValues.count).toBe(''));
    });

    it('shows the Formula switch on a STRING tool field and converts the constant', async () => {
        render(<Wrapper property={uriProperty} value="https://example.com" />);

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await waitFor(() => expect(formValues.uri).toBe("='https://example.com'"));
    });

    it('converts a quoted STRING formula back into the constant', async () => {
        render(<Wrapper property={uriProperty} value="='https://example.com'" />);

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await waitFor(() => expect(formValues.uri).toBe('https://example.com'));
    });

    it('converts a BOOLEAN select constant into a formula', async () => {
        render(<Wrapper property={enabledProperty} value="true" />);

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await waitFor(() => expect(formValues.enabled).toBe('=true'));
    });

    it('converts a BOOLEAN formula back into the selected string option', async () => {
        render(<Wrapper property={enabledProperty} value="=true" />);

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await waitFor(() => expect(formValues.enabled).toBe('true'));

        expect(screen.getByText('True')).toBeInTheDocument();
    });

    it('converts an INTEGER select formula back into the string option value', async () => {
        render(<Wrapper property={prioritySelectProperty} value="=2" />);

        await settle();

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await waitFor(() => expect(formValues.priority).toBe('2'));
    });

    it('typing = into an empty number field enters formula mode', async () => {
        const {container} = render(<Wrapper property={countProperty} value="" />);

        await settle();

        fireEvent.keyDown(container.querySelector('input')!, {key: '='});

        await waitFor(() => expect(container.querySelector('.ProseMirror')).not.toBeNull());

        expect(formValues.count).toBe('=');
    });

    it('typing = into a number field that holds a value does not enter formula mode', async () => {
        const {container} = render(<Wrapper property={countProperty} value={5} />);

        await settle();

        fireEvent.keyDown(container.querySelector('input')!, {key: '='});

        await settle();

        expect(container.querySelector('.ProseMirror')).toBeNull();
        expect(formValues.count).toBe(5);
    });

    it('hands the caret to the formula editor when = is typed into an empty number field', async () => {
        const {container} = render(<Wrapper property={countProperty} value="" />);

        await settle();

        const focusSpy = vi.spyOn(HTMLElement.prototype, 'focus');

        fireEvent.keyDown(container.querySelector('input')!, {key: '='});

        await settle();

        expect(focusSpy.mock.instances).toContain(container.querySelector('.ProseMirror'));
    });

    it('returns an emptied number formula to the plain input', async () => {
        const {container} = render(<Wrapper property={countProperty} value="" />);

        await settle();

        fireEvent.keyDown(container.querySelector('input')!, {key: '='});

        await settle();

        fireEvent.keyDown(container.querySelector('.ProseMirror')!, {
            charCode: 8,
            code: 'Backspace',
            key: 'Backspace',
            keyCode: 8,
        });

        await waitFor(() => expect(container.querySelector('.ProseMirror')).toBeNull());

        expect(container.querySelector('input')).not.toBeNull();
        expect(formValues.count).toBe('');
    });

    it('keeps a number formula field in the editor when fromAi is turned on', async () => {
        const {container} = render(<Wrapper property={countProperty} value="=5" />);

        await settle();

        fireEvent.click(container.querySelector('.lucide-sparkles')!.closest('button')!);

        await waitFor(() => expect(screen.getByText('Automatically defined by the model')).toBeInTheDocument());

        expect(screen.queryByRole('switch', {name: 'Formula'})).toBeNull();
        expect(String(formValues.count)).toMatch(/^=fromAi\('count'/);
    });

    it('converts a STRING expression typed into the plain input back into text', async () => {
        const {container} = render(<Wrapper property={uriProperty} value="" />);

        await settle();

        fireEvent.change(container.querySelector('input')!, {target: {value: "='abc'"}});

        await waitFor(() => expect(container.querySelector('.ProseMirror')).not.toBeNull());

        fireEvent.click(screen.getByRole('switch', {name: 'Formula'}));

        await waitFor(() => expect(formValues.uri).toBe('abc'));

        expect(container.querySelector('.ProseMirror')).toBeNull();
    });

    it('keeps a non-string fromAi value out of formula mode and hides the switch', async () => {
        render(<Wrapper property={countProperty} value="=fromAi('count', 'INTEGER', {'required': false})" />);

        await settle();

        expect(screen.queryByRole('switch', {name: 'Formula'})).toBeNull();
        expect(screen.getByText('Automatically defined by the model')).toBeInTheDocument();
    });

    it('hides the switch when the surface disables formulas', async () => {
        render(<Wrapper formulaEnabled={false} property={countProperty} value={5} />);

        await settle();

        expect(screen.queryByRole('switch', {name: 'Formula'})).toBeNull();
    });

    it('hides the switch on a controlled form that is not a tool', async () => {
        render(<Wrapper property={countProperty} toolsMode={false} value={5} />);

        await settle();

        expect(screen.queryByRole('switch', {name: 'Formula'})).toBeNull();
    });
});
