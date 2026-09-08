vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ERROR_MESSAGES} from '@/shared/errorMessages';
import {PropertyAllType} from '@/shared/types';
import {render, screen} from '@/shared/util/test-utils';
import {fireEvent, waitFor} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

/**
 * A required controlled property has to say so when a submit is blocked. The rule carried no message, and the
 * text area passed no field state through at all, so Save looked like a button that did nothing.
 */

const Wrapper = ({property}: {property: PropertyAllType}) => {
    const form = useForm({defaultValues: {toolDescription: '', toolName: ''}});

    return (
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <form noValidate onSubmit={form.handleSubmit(() => {})}>
                    <Property
                        control={form.control as never}
                        controlPath=""
                        formState={form.formState}
                        property={property}
                        toolsMode
                    />

                    <button type="submit">Save</button>
                </form>
            </WorkflowEditorProvider>
        </TooltipProvider>
    );
};

describe('required controlled properties', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-required', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: undefined,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('reports a blocked submit on a required text property', async () => {
        render(
            <Wrapper
                property={
                    {
                        controlType: 'TEXT',
                        label: 'Name',
                        name: 'toolName',
                        required: true,
                        type: 'STRING',
                    } as PropertyAllType
                }
            />
        );

        fireEvent.click(screen.getByText('Save'));

        await waitFor(() =>
            expect(screen.getByRole('alert')).toHaveTextContent(ERROR_MESSAGES.PROPERTY.FIELD_REQUIRED)
        );
    });

    // The text area was the one control that passed no field state through, so its block was silent.
    it('reports a blocked submit on a required text area property', async () => {
        render(
            <Wrapper
                property={
                    {
                        controlType: 'TEXT_AREA',
                        label: 'Description',
                        name: 'toolDescription',
                        required: true,
                        type: 'STRING',
                    } as PropertyAllType
                }
            />
        );

        fireEvent.click(screen.getByText('Save'));

        await waitFor(() =>
            expect(screen.getByRole('alert')).toHaveTextContent(ERROR_MESSAGES.PROPERTY.FIELD_REQUIRED)
        );
    });
});
