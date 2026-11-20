vi.mock('@/assets/TwoHashesIcon.svg?react', () => ({
    default: () => <svg />,
}));

vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {render} from '@/shared/util/test-utils';
import {screen, waitFor} from '@testing-library/react';
import {useEffect} from 'react';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

let formValues: Record<string, unknown> = {};

const LateSeededWrapper = ({property, seededValue}: {property: PropertyAllType; seededValue: string}) => {
    const form = useForm<Record<string, unknown>>({defaultValues: {}});

    formValues = form.watch();

    useEffect(() => {
        const timeoutId = setTimeout(() => form.reset({[property.name!]: seededValue}, {keepDirtyValues: true}), 20);

        return () => clearTimeout(timeoutId);
    }, [form, property.name, seededValue]);

    return (
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
    );
};

const lateSeededCases: Array<{property: PropertyAllType; seededValue: string}> = [
    {
        property: {controlType: 'INTEGER', expressionEnabled: true, label: 'TopK', name: 'topK', type: 'INTEGER'},
        seededValue: "=fromAi('topK', 'INTEGER', {'required': false})",
    },
    {
        property: {controlType: 'NUMBER', expressionEnabled: true, label: 'Score', name: 'score', type: 'NUMBER'},
        seededValue: "=fromAi('score', 'NUMBER', {'required': false})",
    },
    {
        property: {controlType: 'DATE', expressionEnabled: true, label: 'Day', name: 'day', type: 'DATE'},
        seededValue: "=fromAi('day', 'DATE', {'required': false})",
    },
    {
        property: {
            controlType: 'DATE_TIME',
            expressionEnabled: true,
            label: 'At',
            name: 'at',
            type: 'DATE_TIME',
        },
        seededValue: "=fromAi('at', 'DATE_TIME', {'required': false})",
    },
    {
        property: {controlType: 'TIME', expressionEnabled: true, label: 'Hour', name: 'hour', type: 'TIME'},
        seededValue: "=fromAi('hour', 'TIME', {'required': false})",
    },
    {
        property: {controlType: 'SELECT', expressionEnabled: true, label: 'Enabled', name: 'enabled', type: 'BOOLEAN'},
        seededValue: "=fromAi('enabled', 'BOOLEAN', {'options': {'true', 'false'}, 'required': false})",
    },
    {
        property: {
            controlType: 'SELECT',
            expressionEnabled: true,
            label: 'Priority',
            name: 'priority',
            options: [
                {label: 'One', value: '1'},
                {label: 'Two', value: '2'},
            ],
            type: 'INTEGER',
        },
        seededValue: "=fromAi('priority', 'INTEGER', {'options': {'1', '2'}, 'required': false})",
    },
    {
        property: {controlType: 'TEXT', expressionEnabled: true, label: 'Query', name: 'query', type: 'STRING'},
        seededValue: "=fromAi('query', 'STRING', {'required': false})",
    },
] as Array<{property: PropertyAllType; seededValue: string}>;

describe('controlled tool field seeded with fromAi after mount', () => {
    beforeEach(() => {
        formValues = {};

        useWorkflowDataStore.setState({
            workflow: {id: 'wf-late-from-ai', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: undefined,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it.each(lateSeededCases)(
        'shows a late fromAi value of a $property.type $property.controlType field',
        async ({property, seededValue}) => {
            render(<LateSeededWrapper property={property} seededValue={seededValue} />);

            await waitFor(() => expect(formValues[property.name!]).toBe(seededValue), {interval: 5, timeout: 5000});

            await waitFor(() => expect(screen.getByText('Automatically defined by the model')).toBeInTheDocument(), {
                interval: 5,
                timeout: 5000,
            });

            expect(formValues[property.name!]).toBe(seededValue);
        }
    );
});
