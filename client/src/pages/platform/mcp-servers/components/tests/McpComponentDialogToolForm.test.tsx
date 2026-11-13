import {TooltipProvider} from '@/components/ui/tooltip';
import {pickToolFormChanges} from '@/pages/platform/mcp-servers/hooks/useMcpComponentToolForms';
import buildToolParametersPayload from '@/pages/platform/mcp-servers/utils/buildToolParametersPayload';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {act, render, resetAll, screen, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentDialogToolForm, {McpToolFormRegistrationI} from '../McpComponentDialogToolForm';

const hoisted = vi.hoisted(() => ({
    clusterElementDefinitionResult: {
        data: {clusterElementDefinition: {properties: [] as Array<Record<string, unknown>>}},
        isLoading: false,
    },
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useClusterElementDefinitionQuery: () => hoisted.clusterElementDefinitionResult,
}));

vi.mock('@/shared/queries/platform/useFormDisplayConditions', () => ({
    default: () => ({displayConditions: undefined, isEvaluating: false}),
}));

vi.mock('@/pages/platform/mcp-servers/hooks/useMcpToolOptionsLoader', () => ({
    default: () => () => Promise.resolve(null),
}));

const multiSelectProperty = {
    controlType: 'MULTI_SELECT',
    name: 'labels',
    options: [
        {label: 'Bug', value: 'bug'},
        {label: 'Feature', value: 'feature'},
    ],
    type: 'ARRAY',
};

const bodyProperty = {
    controlType: 'OBJECT_BUILDER',
    name: 'body',
    properties: [
        {controlType: 'TEXT', defaultValue: 'JSON', name: 'format', type: 'STRING'},
        {controlType: 'TEXT', name: 'content', type: 'STRING'},
    ],
    type: 'OBJECT',
};

const subjectProperty = {controlType: 'TEXT', label: 'Subject', name: 'subject', type: 'STRING'};

interface RenderOptionsI {
    isNew?: boolean;
    properties: Array<Record<string, unknown>>;
    savedParameters?: Record<string, unknown> | null;
}

const renderToolForm = ({isNew = false, properties, savedParameters}: RenderOptionsI) => {
    hoisted.clusterElementDefinitionResult = {
        data: {clusterElementDefinition: {properties}},
        isLoading: false,
    };

    const registrations: Array<McpToolFormRegistrationI | null> = [];

    render(
        <TooltipProvider>
            <WorkflowReadOnlyProvider value={{useGetComponentDefinitionsQuery: () => ({data: []})} as never}>
                <McpComponentDialogToolForm
                    componentName="github"
                    componentVersion={1}
                    isNew={isNew}
                    onFormChange={(_toolName, registration) => registrations.push(registration)}
                    savedParameters={savedParameters}
                    toolName="createIssue"
                />
            </WorkflowReadOnlyProvider>
        </TooltipProvider>
    );

    const buildPayload = () => {
        const registration = registrations[registrations.length - 1];

        return buildToolParametersPayload({
            aiValues: {},
            dirtyValues: registration ? pickToolFormChanges(registration) : {},
            savedParameters,
        });
    };

    return {buildPayload, registrations};
};

describe('McpComponentDialogToolForm', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('does not save an untouched multi select without a default', async () => {
        const {buildPayload, registrations} = renderToolForm({isNew: true, properties: [multiSelectProperty]});

        expect(await screen.findByText('Select...')).toBeInTheDocument();

        await waitFor(() => {
            expect(registrations[registrations.length - 1]).toBeTruthy();
        });

        act(() => {
            registrations[registrations.length - 1]?.form.setValue('labels', []);
        });

        expect(buildPayload()).toEqual({});
    });

    it('saves a saved object as it is without injecting a nested default it hides', async () => {
        const {buildPayload, registrations} = renderToolForm({
            properties: [bodyProperty],
            savedParameters: {body: {content: 'x'}},
        });

        expect(await screen.findByDisplayValue('JSON')).toBeInTheDocument();

        await waitFor(() => {
            expect(registrations[registrations.length - 1]).toBeTruthy();
        });

        act(() => {
            registrations[registrations.length - 1]?.form.setValue('body.format', 'JSON' as never);
        });

        expect(buildPayload()).toEqual({body: {content: 'x'}});
    });

    it('saves a field the user cleared', async () => {
        const user = userEvent.setup();

        const {buildPayload, registrations} = renderToolForm({
            properties: [subjectProperty],
            savedParameters: {subject: 'Hello'},
        });

        const subjectInput = await screen.findByDisplayValue('Hello');

        await user.clear(subjectInput);

        await waitFor(() => {
            expect(registrations[registrations.length - 1]?.form.getValues('subject')).toBe('');
        });

        expect(buildPayload()).toEqual({subject: null});
    });
});
