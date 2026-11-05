vi.mock('@/pages/platform/workflow-editor/utils/saveProperty', () => ({
    default: vi.fn(),
}));

import {TooltipProvider} from '@/components/ui/tooltip';
import {workflowEditorProviderTestValue} from '@/pages/platform/workflow-editor/providers/tests/workflowEditorProviderTestValue';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import saveProperty from '@/pages/platform/workflow-editor/utils/saveProperty';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {act, fireEvent, waitFor} from '@testing-library/react';
import {Editor} from '@tiptap/react';
import {ComponentProps} from 'react';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyMentionsInputEditor from '../PropertyMentionsInputEditor';

const DATA_PILLS: DataPillType[] = [
    {id: 'trigger_1', value: 'trigger_1'},
    {id: 'trigger_1.name', value: 'trigger_1.name'},
    {id: 'trigger_1.first-name', value: 'trigger_1.first-name'},
];

const SAVE_WAIT_OPTIONS = {timeout: 3000};

type EditorPropsType = ComponentProps<typeof PropertyMentionsInputEditor>;

const savedValues = () => (saveProperty as unknown as Mock).mock.calls.map(([options]) => options.value);

const renderEditor = (props: Partial<EditorPropsType> = {}) => {
    const onChange = vi.fn();
    const onValueChange = vi.fn();

    const view = render(
        <TooltipProvider>
            <WorkflowEditorProvider value={workflowEditorProviderTestValue as never}>
                <span id="field-label">Message</span>

                <PropertyMentionsInputEditor
                    componentDefinitions={[]}
                    controlType="TEXT"
                    dataPills={DATA_PILLS}
                    expressionEnabled
                    labelId="field-label"
                    onChange={onChange}
                    onValueChange={onValueChange}
                    path="parameters.message"
                    taskDispatcherDefinitions={[]}
                    type="STRING"
                    value=""
                    workflow={{id: 'workflow-1'} as Workflow}
                    {...props}
                />
            </WorkflowEditorProvider>
        </TooltipProvider>
    );

    const findTextbox = () => screen.findByRole('textbox', {name: 'Message'});

    return {findTextbox, onChange, onValueChange, view};
};

const getSuggestionMenu = () => document.querySelector('.property-mentions-suggestion-menu');

beforeEach(() => {
    (saveProperty as unknown as Mock).mockReset();

    useWorkflowDataStore.setState({
        componentDefinitions: [],
        dataPills: DATA_PILLS,
        taskDispatcherDefinitions: [],
        workflow: {id: 'workflow-1', nodeNames: ['trigger_1']},
    } as unknown as Partial<ReturnType<typeof useWorkflowDataStore.getState>>);

    useWorkflowNodeDetailsPanelStore.setState({
        currentNode: {connectionId: undefined, workflowNodeName: 'slack_1'},
        pillTarget: null,
    } as unknown as Partial<ReturnType<typeof useWorkflowNodeDetailsPanelStore.getState>>);
});

describe('PropertyMentionsInputEditor behaviour', () => {
    it('renders a stored value with its data pills between the text', async () => {
        const {findTextbox} = renderEditor({value: 'Hello ${trigger_1.name} and ${trigger_1}!'});

        const textbox = await findTextbox();

        await waitFor(() => {
            expect(textbox.querySelectorAll('.property-mention')).toHaveLength(2);
        });

        expect(textbox).toHaveTextContent('Hello trigger_1.name and trigger_1!');
    });

    describe('typing', () => {
        it('types into the empty line of a clicked empty field instead of opening a new one', async () => {
            const {findTextbox, onValueChange} = renderEditor();

            const textbox = await findTextbox();

            await userEvent.click(textbox);
            await userEvent.keyboard('Hi');

            expect(onValueChange).toHaveBeenLastCalledWith('Hi');
            expect(textbox.querySelectorAll('p')).toHaveLength(1);
        });

        it('saves the typed text once typing pauses and reports every change', async () => {
            const {findTextbox, onChange, onValueChange} = renderEditor();

            await userEvent.click(await findTextbox());
            await userEvent.keyboard('Hello');

            await waitFor(() => {
                expect(savedValues()).toEqual(['Hello']);
            }, SAVE_WAIT_OPTIONS);

            expect(saveProperty).toHaveBeenCalledWith(
                expect.objectContaining({
                    includeInMetadata: true,
                    path: 'parameters.message',
                    type: 'STRING',
                    workflowId: 'workflow-1',
                })
            );
            expect(onChange).toHaveBeenLastCalledWith('Hello');
            expect(onValueChange).toHaveBeenLastCalledWith('Hello');
        });

        it('saves characters that look like markup literally', async () => {
            const {findTextbox} = renderEditor();

            await userEvent.click(await findTextbox());
            await userEvent.keyboard('a < b & c');

            await waitFor(() => {
                expect(savedValues()).toEqual(['a < b & c']);
            }, SAVE_WAIT_OPTIONS);
        });

        it('saves text still waiting on the debounce when the editor goes away', async () => {
            const {findTextbox, onValueChange, view} = renderEditor();

            await userEvent.click(await findTextbox());
            await userEvent.keyboard('Bye');

            expect(onValueChange).toHaveBeenLastCalledWith('Bye');

            view.unmount();

            expect(savedValues()).toEqual(['Bye']);
        });

        it('does not save when auto save is off', async () => {
            const {findTextbox, onValueChange, view} = renderEditor({disableAutoSave: true});

            await userEvent.click(await findTextbox());
            await userEvent.keyboard('Draft');

            expect(onValueChange).toHaveBeenLastCalledWith('Draft');

            view.unmount();

            expect(saveProperty).not.toHaveBeenCalled();
        });

        it('does not save a value the field rejects', async () => {
            const validateBeforeSave = vi.fn(() => false);

            const {findTextbox, view} = renderEditor({validateBeforeSave});

            await userEvent.click(await findTextbox());
            await userEvent.keyboard('bad');

            view.unmount();

            expect(validateBeforeSave).toHaveBeenLastCalledWith('bad');
            expect(saveProperty).not.toHaveBeenCalled();
        });

        it('saves a whole number typed into a number field with pills as a number', async () => {
            const {findTextbox, view} = renderEditor({legacyMixed: true, type: 'INTEGER'});

            await userEvent.click(await findTextbox());
            await userEvent.keyboard('42');

            view.unmount();

            expect(savedValues()).toEqual([42]);
        });

        it('keeps the fraction of a decimal typed into a number field with pills', async () => {
            const {findTextbox, view} = renderEditor({legacyMixed: true, type: 'NUMBER'});

            await userEvent.click(await findTextbox());
            await userEvent.keyboard('1.5');

            view.unmount();

            expect(savedValues()).toEqual([1.5]);
        });

        it('takes only a data pill in a number field', async () => {
            const {findTextbox, onValueChange} = renderEditor({type: 'INTEGER'});

            const textbox = await findTextbox();

            await userEvent.click(textbox);
            await userEvent.keyboard('42$');

            await waitFor(() => {
                expect(textbox).toHaveTextContent('$');
            });

            expect(textbox).not.toHaveTextContent('42');
            expect(onValueChange).not.toHaveBeenCalledWith(expect.stringContaining('4'));
        });
    });

    describe('formula mode', () => {
        it('saves the typed expression with its = prefix and validates it that way', async () => {
            const validateBeforeSave = vi.fn(() => true);

            const {findTextbox, view} = renderEditor({isFormulaMode: true, validateBeforeSave});

            await userEvent.click(await findTextbox());
            await userEvent.keyboard('1 + 2');

            view.unmount();

            expect(validateBeforeSave).toHaveBeenLastCalledWith('=1 + 2');
            expect(savedValues()).toEqual(['=1 + 2']);
        });

        it('does not turn an expression in a number field into a number', async () => {
            const {findTextbox, view} = renderEditor({isFormulaMode: true, type: 'INTEGER'});

            await userEvent.click(await findTextbox());
            await userEvent.keyboard('42');

            view.unmount();

            expect(savedValues()).toEqual(['=42']);
        });
    });

    describe('data pills', () => {
        it('turns a data pill picked from the $ suggestions into a pill', async () => {
            const {findTextbox} = renderEditor();

            const textbox = await findTextbox();

            await userEvent.click(textbox);
            await userEvent.keyboard('Hi $trig');

            await waitFor(() => {
                expect(getSuggestionMenu()).toHaveTextContent('trigger_1.name');
            });

            await userEvent.click(screen.getByRole('button', {name: 'trigger_1.name'}));

            await waitFor(() => {
                expect(savedValues()).toEqual(['Hi ${trigger_1.name}']);
            }, SAVE_WAIT_OPTIONS);

            expect(textbox.querySelectorAll('.property-mention')).toHaveLength(1);
        });

        it('quotes a picked data pill whose name is not a plain identifier', async () => {
            const {findTextbox} = renderEditor();

            await userEvent.click(await findTextbox());
            await userEvent.keyboard('$first');

            await waitFor(() => {
                expect(getSuggestionMenu()).toHaveTextContent('trigger_1.first-name');
            });

            await userEvent.keyboard('{Enter}');

            await waitFor(() => {
                expect(savedValues()).toEqual(["${trigger_1['first-name']}"]);
            }, SAVE_WAIT_OPTIONS);
        });

        it('offers no data pill suggestions when expressions are off', async () => {
            const {findTextbox, view} = renderEditor({expressionEnabled: false});

            const textbox = await findTextbox();

            await userEvent.click(textbox);
            await userEvent.keyboard('$trig');

            await waitFor(() => {
                expect(textbox).toHaveTextContent('$trig');
            });

            expect(getSuggestionMenu()).toBeNull();

            view.unmount();

            expect(savedValues()).toEqual(['$trig']);
        });

        it('deletes the pill before the caret on Backspace', async () => {
            let editor: Editor | null = null;

            const {findTextbox} = renderEditor({
                ref: (instance: Editor | null) => {
                    editor = instance;
                },
                value: 'Hi ${trigger_1.name}',
            });

            const textbox = await findTextbox();

            await waitFor(() => {
                expect(textbox.querySelector('.property-mention')).not.toBeNull();
            });

            await userEvent.click(textbox);

            act(() => {
                editor!.commands.focus('end');
            });

            await userEvent.keyboard('{Backspace}');

            expect(textbox.querySelector('.property-mention')).toBeNull();

            await waitFor(() => {
                expect(savedValues()).toEqual(['Hi ']);
            }, SAVE_WAIT_OPTIONS);
        });

        it('removes a pill whose source is gone from its remove button', async () => {
            const {findTextbox} = renderEditor({value: 'Hi ${gone_1.value}'});

            const textbox = await findTextbox();

            await userEvent.click(await screen.findByRole('button', {name: 'Remove unavailable data pill'}));

            expect(textbox.querySelector('.property-mention')).toBeNull();

            await waitFor(() => {
                expect(savedValues()).toEqual(['Hi ']);
            }, SAVE_WAIT_OPTIONS);
        });

        it('inserts a data pill dropped from the data pill panel', async () => {
            const {findTextbox} = renderEditor();

            const textbox = await findTextbox();

            fireEvent.drop(textbox, {
                dataTransfer: {
                    getData: (format: string) =>
                        format === 'application/bytechef-datapill' ? JSON.stringify({mentionId: 'trigger_1.name'}) : '',
                    types: ['application/bytechef-datapill'],
                },
            });

            await waitFor(() => {
                expect(savedValues()).toEqual([expect.stringContaining('${trigger_1.name}')]);
            }, SAVE_WAIT_OPTIONS);

            expect(textbox.querySelectorAll('.property-mention')).toHaveLength(1);
        });

        it('refuses a dropped data pill when expressions are off', async () => {
            const {findTextbox, view} = renderEditor({expressionEnabled: false});

            const textbox = await findTextbox();

            fireEvent.drop(textbox, {
                dataTransfer: {
                    getData: (format: string) =>
                        format === 'application/bytechef-datapill' ? JSON.stringify({mentionId: 'trigger_1.name'}) : '',
                    types: ['application/bytechef-datapill'],
                },
            });

            expect(textbox.querySelector('.property-mention')).toBeNull();

            view.unmount();

            expect(saveProperty).not.toHaveBeenCalled();
        });
    });
});
