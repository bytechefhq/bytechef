import PropertyMentionsInput from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {GetComponentDefinitionsRequestI} from '@/shared/queries/platform/componentDefinitions.queries';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {UseQueryResult} from '@tanstack/react-query';
import {beforeEach, describe, expect, it} from 'vitest';

const renderInput = (expressionEnabled?: boolean) =>
    render(
        <WorkflowReadOnlyProvider
            value={{
                useGetComponentDefinitionsQuery: {} as (
                    request: GetComponentDefinitionsRequestI,
                    enabled?: boolean
                ) => UseQueryResult<Array<ComponentDefinitionBasic>, Error>,
            }}
        >
            <PropertyMentionsInput
                controlType="TEXT"
                defaultValue=""
                expressionEnabled={expressionEnabled}
                handleInputTypeSwitchButtonClick={() => {}}
                label="Property label"
                placeholder=""
                type="STRING"
                value=""
            />
        </WorkflowReadOnlyProvider>
    );

describe('PropertyMentionsInput data pill panel gate', () => {
    beforeEach(() => {
        useDataPillPanelStore.setState({dataPillPanelOpen: false});
        useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: true});
    });

    it('should open the data pill panel when the property accepts expressions', async () => {
        renderInput(true);

        await userEvent.click(screen.getByRole('textbox'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
    });

    it('should open the data pill panel for a property that does not declare the flag', async () => {
        renderInput(undefined);

        await userEvent.click(screen.getByRole('textbox'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(true);
    });

    it('should keep the data pill panel closed for a literals-only property', async () => {
        renderInput(false);

        await userEvent.click(screen.getByRole('textbox'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
    });

    it('should keep the data pill panel closed while the node details panel is closed', async () => {
        useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: false});

        renderInput(true);

        await userEvent.click(screen.getByRole('textbox'));

        expect(useDataPillPanelStore.getState().dataPillPanelOpen).toBe(false);
    });
});
