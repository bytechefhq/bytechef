import {PropertyAllType} from '@/shared/types';
import {render, screen} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {describe, expect, it, vi} from 'vitest';

import Properties from '../Properties';

// useProperty is mocked per property so nested Property instances (object sub-properties, array items, dynamic
// sub-properties) each resolve their own name and displayCondition — the assertions are about the gate, not
// about the inputs.
vi.mock('@/pages/platform/workflow-editor/components/properties/hooks/useProperty', () => ({
    default: ({controlPath, property}: {controlPath?: string; property: PropertyAllType}) => ({
        calculatedPath: `${controlPath ?? 'parameters'}.${property.name}`,
        controlType: property.controlType ?? 'TEXT',
        controlledDynamicMode: false,
        controlledDynamicOnChangeRef: {current: undefined},
        currentNode: undefined,
        displayCondition: property.displayCondition,
        editorRef: {current: null},
        formattedOptions: [],
        fromAiExpression: '',
        handleMentionInputValueChange: vi.fn(),
        isLoadingDisplayCondition: false,
        isValidControlType: true,
        label: property.label ?? property.name,
        mentionInput: false,
        name: property.name,
        setIsFormulaMode: vi.fn(),
        setLookupDependsOnValues: vi.fn(),
        type: property.type,
        workflow: {id: 'workflow-1'},
    }),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/hooks/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: {currentNode: undefined}) => unknown) => selector({currentNode: undefined}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

const bodyProperty = {
    controlType: 'OBJECT_BUILDER',
    name: 'body',
    properties: [
        {controlType: 'TEXT', name: 'bodyContentType', type: 'STRING'},
        {controlType: 'TEXT', displayCondition: "body.bodyContentType == 'JSON'", name: 'bodyContent', type: 'STRING'},
        {controlType: 'TEXT', displayCondition: "body.bodyContentType == 'RAW'", name: 'rawContent', type: 'STRING'},
    ],
    type: 'OBJECT',
} as unknown as PropertyAllType;

const Wrapper = ({formDisplayConditions}: {formDisplayConditions?: Record<string, boolean>}) => {
    const form = useForm({defaultValues: {parameters: {body: {bodyContentType: 'JSON'}}}});

    return (
        <Properties
            control={form.control}
            controlPath="parameters"
            formDisplayConditions={formDisplayConditions}
            formState={form.formState}
            properties={[bodyProperty]}
        />
    );
};

describe('Property form display conditions', () => {
    it('keeps every nested conditional property visible while conditions are unevaluated', () => {
        render(<Wrapper />);

        expect(screen.getByLabelText('bodyContent property')).toBeInTheDocument();
        expect(screen.getByLabelText('rawContent property')).toBeInTheDocument();
    });

    it('gates nested object sub-properties on the evaluated form display conditions', () => {
        render(<Wrapper formDisplayConditions={{"body.bodyContentType == 'JSON'": true}} />);

        expect(screen.getByLabelText('bodyContentType property')).toBeInTheDocument();
        expect(screen.getByLabelText('bodyContent property')).toBeInTheDocument();
        expect(screen.queryByLabelText('rawContent property')).not.toBeInTheDocument();
    });
});
