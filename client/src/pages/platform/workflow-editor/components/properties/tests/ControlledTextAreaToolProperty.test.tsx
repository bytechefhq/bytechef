vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {TooltipProvider} from '@/components/ui/tooltip';
import Property from '@/pages/platform/workflow-editor/components/properties/Property';
import {TOOL_PROPERTY_PLACEHOLDER} from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/mentionsInputPlaceholder';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {PropertyAllType} from '@/shared/types';
import {render, screen} from '@/shared/util/test-utils';
import {fireEvent, waitFor} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const bodyProperty = {
    controlType: 'TEXT_AREA',
    label: 'Body',
    name: 'body',
    placeholder: 'Write the email body',
    type: 'STRING',
} as PropertyAllType;

const Wrapper = ({property = bodyProperty}: {property?: PropertyAllType}) => {
    const form = useForm({defaultValues: {body: ''}});

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
                </form>
            </WorkflowEditorProvider>
        </TooltipProvider>
    );
};

describe('controlled text area tool property', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({
            workflow: {id: 'wf-text-area-tool', nodeNames: []},
        } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: undefined,
        } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
    });

    it('offers the from AI toggle', () => {
        const {container} = render(<Wrapper />);

        expect(container.querySelector('.lucide-sparkles')).toBeInTheDocument();
    });

    it('shows the tool property hint rather than the defined placeholder', () => {
        render(<Wrapper />);

        expect(screen.getByPlaceholderText(TOOL_PROPERTY_PLACEHOLDER)).toBeInTheDocument();
    });

    it('hands the field to the model when the toggle is clicked', async () => {
        const {container} = render(<Wrapper />);

        fireEvent.click(container.querySelector('.lucide-sparkles')!.closest('button')!);

        await waitFor(() => expect(screen.getByText('Automatically defined by the model')).toBeInTheDocument());
    });

    it('does not offer the from AI toggle when expressions are disabled', () => {
        const {container} = render(
            <Wrapper property={{...bodyProperty, expressionEnabled: false} as PropertyAllType} />
        );

        expect(container.querySelector('.lucide-sparkles')).not.toBeInTheDocument();
    });

    it('does not offer the from AI toggle outside a tool', () => {
        const {container} = renderOutsideTool();

        expect(container.querySelector('.lucide-sparkles')).not.toBeInTheDocument();
    });
});

function renderOutsideTool() {
    const PlainWrapper = () => {
        const form = useForm({defaultValues: {body: ''}});

        return (
            <TooltipProvider>
                <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                    <form noValidate onSubmit={form.handleSubmit(() => {})}>
                        <Property
                            control={form.control as never}
                            controlPath=""
                            formState={form.formState}
                            property={bodyProperty}
                        />
                    </form>
                </WorkflowEditorProvider>
            </TooltipProvider>
        );
    };

    return render(<PlainWrapper />);
}
