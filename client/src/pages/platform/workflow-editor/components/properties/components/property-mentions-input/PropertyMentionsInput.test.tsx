import PropertyMentionsInput from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInput';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {GetComponentDefinitionsRequestI} from '@/shared/queries/platform/componentDefinitions.queries';
import {render, screen} from '@/shared/util/test-utils';
import {UseQueryResult} from '@tanstack/react-query';
import {describe, expect, test, vi} from 'vitest';

describe('PropertyMentionsInput', () => {
    test('renders without errors', () => {
        const mockFunction = vi.fn();

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
                    handleInputTypeSwitchButtonClick={mockFunction}
                    label="PropertyMentionsInput Label"
                    leadingIcon="📄"
                    placeholder=""
                    type="STRING"
                    value=""
                />
            </WorkflowReadOnlyProvider>
        );

        expect(screen.getByText('PropertyMentionsInput Label')).toBeInTheDocument();
    });
});
