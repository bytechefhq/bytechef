vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({default: vi.fn()}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {act, fireEvent, screen, waitFor} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

const countProperty = {
    controlType: 'INTEGER',
    expressionEnabled: true,
    label: 'Count',
    name: 'count',
    type: 'INTEGER',
} as PropertyAllType;

const enabledProperty = {
    controlType: 'SELECT',
    expressionEnabled: true,
    label: 'Enabled',
    name: 'enabled',
    type: 'BOOLEAN',
} as PropertyAllType;

let formValues: Record<string, unknown> = {};

const settle = async () => {
    for (let index = 0; index < 4; index++) {
        await act(async () => {
            await new Promise<void>((resolve) => setTimeout(resolve, 0));
        });
    }

    await act(async () => {
        await new Promise<void>((resolve) => setTimeout(resolve, 120));
    });
};

const setCurrentNode = (clusterElementType: string | undefined, parameters: Record<string, unknown>) =>
    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {clusterElementType, name: 'node_1', parameters, workflowNodeName: 'node_1'},
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);

const renderUncontrolled = (property: PropertyAllType, value: unknown, {hideFromAi = false, tools = true} = {}) => {
    setCurrentNode(tools ? 'tools' : undefined, {[property.name!]: value});

    return render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property hideFromAi={hideFromAi} parameterValue={value} path={property.name} property={property} />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );
};

const ControlledWrapper = ({
    property,
    toolsMode = true,
    value,
}: {
    property: PropertyAllType;
    toolsMode?: boolean;
    value: unknown;
}) => {
    const form = useForm({defaultValues: {[property.name!]: value}});

    formValues = form.watch();

    return (
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <Property
                    control={form.control as never}
                    controlPath=""
                    formState={form.formState}
                    property={property}
                    toolsMode={toolsMode}
                />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );
};

const getFromAiButton = (container: HTMLElement) =>
    container.querySelector('.lucide-sparkles')?.closest('button') ?? null;

const expectFromAiRendering = (container: HTMLElement) => {
    expect(screen.getByText('Automatically defined by the model')).toBeInTheDocument();
    expect(container.querySelector('.lucide-square-function')).not.toBeNull();
    expect(screen.queryByRole('switch', {name: 'Formula'})).toBeNull();
};

describe('fromAi button on native tool fields in Text mode', () => {
    beforeEach(() => {
        formValues = {};

        (saveProperty as unknown as Mock).mockReset();

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-native-from-ai', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined, pillTarget: null});
    });

    it.each([
        ['INTEGER', countProperty, 5],
        ['BOOLEAN', enabledProperty, true],
    ])('an uncontrolled %s tool field turns fromAi on from Text mode', async (_type, property, value) => {
        const {container} = renderUncontrolled(property as PropertyAllType, value);

        await settle();

        expect(screen.getByRole('switch', {name: 'Formula'})).toHaveAttribute('aria-checked', 'false');

        const fromAiButton = getFromAiButton(container);

        expect(fromAiButton).not.toBeNull();

        fireEvent.click(fromAiButton!);

        await settle();

        expectFromAiRendering(container);
        expect(saveProperty).toHaveBeenLastCalledWith(
            expect.objectContaining({fromAi: true, path: (property as PropertyAllType).name})
        );
    });

    it.each([
        ['INTEGER', countProperty, 5],
        ['BOOLEAN', enabledProperty, 'true'],
    ])('a controlled %s tool field turns fromAi on from Text mode', async (_type, property, value) => {
        const {container} = render(<ControlledWrapper property={property as PropertyAllType} value={value} />);

        await settle();

        const fromAiButton = getFromAiButton(container);

        expect(fromAiButton).not.toBeNull();

        fireEvent.click(fromAiButton!);

        await waitFor(() => expect(String(formValues[(property as PropertyAllType).name!])).toMatch(/^=fromAi\(/));

        await settle();

        expectFromAiRendering(container);
    });

    it('is absent on a field that is not a tool property', async () => {
        const {container} = renderUncontrolled(countProperty, 5, {tools: false});

        await settle();

        expect(getFromAiButton(container)).toBeNull();
    });

    it('is absent on a tool field with hideFromAi', async () => {
        const {container} = renderUncontrolled(countProperty, 5, {hideFromAi: true});

        await settle();

        expect(getFromAiButton(container)).toBeNull();
    });

    it('is absent on a controlled field outside tools mode', async () => {
        const {container} = render(<ControlledWrapper property={countProperty} toolsMode={false} value={5} />);

        await settle();

        expect(getFromAiButton(container)).toBeNull();
    });
});
