import {ControlType, PropertyType, WorkflowInput} from '@/shared/middleware/platform/configuration';
import {render, screen} from '@testing-library/react';
import {useForm} from 'react-hook-form';
import {describe, expect, it} from 'vitest';

import InputConfigurationList, {
    resolveBackingWorkflowNodeName,
    resolveComponentInputGroup,
} from './InputConfigurationList';

import type {ComponentConnection} from '@/shared/middleware/automation/configuration';
import type {ComponentDefinition} from '@/shared/middleware/platform/configuration';

const componentDefinition: ComponentDefinition = {
    inputs: [
        {
            name: 'spreadsheetId',
            properties: [
                {
                    controlType: ControlType.Select,
                    label: 'Spreadsheet',
                    name: 'spreadsheetId',
                    type: PropertyType.String,
                } as never,
            ],
        },
        {
            label: 'Date Range',
            name: 'dateRange',
            properties: [
                {label: 'Start', name: 'start', type: PropertyType.String} as never,
                {label: 'End', name: 'end', type: PropertyType.String} as never,
            ],
        },
    ],
    name: 'myComponent',
    title: 'My Component',
} as unknown as ComponentDefinition;

describe('resolveComponentInputGroup', () => {
    it('resolves a single-property group keeping the input name and the single member', () => {
        const input: WorkflowInput = {
            componentReference: {componentName: 'myComponent', componentVersion: 1, groupName: 'spreadsheetId'},
            label: 'Sheet',
            name: 'sheet',
        };

        const group = resolveComponentInputGroup(input, componentDefinition);

        expect(group.name).toBe('sheet');
        expect(group.label).toBe('Sheet');
        expect(group.members.map((member) => member.name)).toEqual(['spreadsheetId']);
        expect(group.members[0].controlType).toBe(ControlType.Select);
        expect(group.members.every((member) => !member.disabled)).toBe(true);
    });

    it('resolves a multi-property group returning its members keeping their own names', () => {
        const input: WorkflowInput = {
            componentReference: {componentName: 'myComponent', componentVersion: 1, groupName: 'dateRange'},
            label: 'Range',
            name: 'range',
        };

        const group = resolveComponentInputGroup(input, componentDefinition);

        expect(group.name).toBe('range');
        expect(group.label).toBe('Range');
        expect(group.members.map((member) => member.name)).toEqual(['start', 'end']);
        expect(group.members.every((member) => !member.disabled)).toBe(true);
    });

    it('falls back to the group label when the input has none', () => {
        const input: WorkflowInput = {
            componentReference: {componentName: 'myComponent', componentVersion: 1, groupName: 'dateRange'},
            name: 'range',
        };

        const group = resolveComponentInputGroup(input, componentDefinition);

        expect(group.label).toBe('Date Range');
    });

    it('returns a disabled placeholder member when the referenced group is no longer available', () => {
        const input: WorkflowInput = {
            componentReference: {componentName: 'myComponent', componentVersion: 1, groupName: 'goneGroup'},
            label: 'Missing',
            name: 'missing',
        };

        const group = resolveComponentInputGroup(input, componentDefinition);

        expect(group.name).toBe('missing');
        expect(group.members).toHaveLength(1);
        expect(group.members[0].disabled).toBe(true);
        expect(group.members[0].controlType).toBe(ControlType.Text);
        expect(group.members[0].description).toBe('This input is no longer available.');
    });

    it('returns a disabled placeholder when the component definition is missing', () => {
        const input: WorkflowInput = {
            componentReference: {componentName: 'myComponent', componentVersion: 1, groupName: 'dateRange'},
            name: 'range',
        };

        const group = resolveComponentInputGroup(input, undefined);

        expect(group.members[0].disabled).toBe(true);
    });
});

describe('resolveBackingWorkflowNodeName', () => {
    const componentConnections: ComponentConnection[] = [
        {componentName: 'other', componentVersion: 1, key: 'k0', workflowNodeName: 'other_1'} as ComponentConnection,
        {
            componentName: 'myComponent',
            componentVersion: 1,
            key: 'k1',
            workflowNodeName: 'myComponent_1',
        } as ComponentConnection,
        {
            componentName: 'myComponent',
            componentVersion: 1,
            key: 'k2',
            workflowNodeName: 'myComponent_2',
        } as ComponentConnection,
    ];

    it('returns the first matching node whose deployment connection is configured', () => {
        const nodeName = resolveBackingWorkflowNodeName('myComponent', componentConnections, [10, undefined, 20]);

        expect(nodeName).toBe('myComponent_2');
    });

    it('returns the first configured node deterministically when several match', () => {
        const nodeName = resolveBackingWorkflowNodeName('myComponent', componentConnections, [10, 11, 20]);

        expect(nodeName).toBe('myComponent_1');
    });

    it('returns undefined when no matching node has a configured connection', () => {
        const nodeName = resolveBackingWorkflowNodeName('myComponent', componentConnections, [
            10,
            undefined,
            undefined,
        ]);

        expect(nodeName).toBeUndefined();
    });

    it('returns undefined when the component has no connection nodes', () => {
        expect(resolveBackingWorkflowNodeName('absent', componentConnections, [10, 11, 20])).toBeUndefined();
    });
});

describe('InputConfigurationList empty state', () => {
    const Harness = ({emptyStateMessage, onOpenInputs}: {emptyStateMessage?: string; onOpenInputs?: () => void}) => {
        const {control, formState} = useForm();

        return (
            <InputConfigurationList
                control={control}
                controlPath="inputs"
                emptyStateMessage={emptyStateMessage}
                formState={formState}
                inputs={[]}
                onOpenInputs={onOpenInputs}
            />
        );
    };

    it('offers to open the inputs sheet when the workflow owns its inputs', () => {
        render(<Harness onOpenInputs={() => {}} />);

        expect(screen.getByRole('button', {name: /open inputs/i})).toBeInTheDocument();
        expect(screen.getByText(/discard the current configuration/i)).toBeInTheDocument();
    });

    it('explains where inputs come from instead of a dead-end button when a message is given', () => {
        render(<Harness emptyStateMessage="Declare them in the workflow's source." onOpenInputs={() => {}} />);

        expect(screen.getByText("Declare them in the workflow's source.")).toBeInTheDocument();

        // The sheet behind that button can only repeat this message, and the discard warning is beside the point.
        expect(screen.queryByRole('button', {name: /open inputs/i})).not.toBeInTheDocument();
        expect(screen.queryByText(/discard the current configuration/i)).not.toBeInTheDocument();
    });
});
