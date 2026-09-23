import {resolveBackingWorkflowNodeName, resolveComponentInputGroup} from '@/shared/components/InputConfigurationList';
import {ComponentConnection} from '@/shared/middleware/automation/configuration';
import {
    ComponentDefinition,
    ControlType,
    PropertyType,
    WorkflowInput,
} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

const componentReferenceInput = (overrides: Partial<WorkflowInput> = {}): WorkflowInput =>
    ({
        componentReference: {componentName: 'hubspot', componentVersion: 1, groupName: 'contactFields'},
        name: 'contact',
        ...overrides,
    }) as WorkflowInput;

describe('resolveComponentInputGroup', () => {
    it('returns the referenced group members with the group label when the input has none', () => {
        const componentDefinition = {
            inputs: [
                {
                    label: 'Contact fields',
                    name: 'contactFields',
                    properties: [{name: 'email', type: PropertyType.String}],
                },
            ],
        } as unknown as ComponentDefinition;

        const resolvedGroup = resolveComponentInputGroup(componentReferenceInput(), componentDefinition);

        expect(resolvedGroup.label).toBe('Contact fields');
        expect(resolvedGroup.name).toBe('contact');
        expect(resolvedGroup.members).toEqual([{name: 'email', type: PropertyType.String}]);
    });

    it('prefers the input label and tolerates a group without properties', () => {
        const componentDefinition = {
            inputs: [{label: 'Contact fields', name: 'contactFields'}],
        } as unknown as ComponentDefinition;

        const resolvedGroup = resolveComponentInputGroup(
            componentReferenceInput({label: 'Customer contact'}),
            componentDefinition
        );

        expect(resolvedGroup.label).toBe('Customer contact');
        expect(resolvedGroup.members).toEqual([]);
    });

    it('returns a disabled placeholder when the referenced group no longer exists', () => {
        const resolvedGroup = resolveComponentInputGroup(componentReferenceInput(), {
            inputs: [],
        } as unknown as ComponentDefinition);

        expect(resolvedGroup.label).toBe('contact');
        expect(resolvedGroup.members).toEqual([
            {
                controlType: ControlType.Text,
                description: 'This input is no longer available.',
                disabled: true,
                label: 'contact',
                name: 'contact',
                type: PropertyType.String,
            },
        ]);
    });

    it('returns a placeholder when the component definition is not loaded', () => {
        const resolvedGroup = resolveComponentInputGroup(componentReferenceInput({label: 'Customer contact'}));

        expect(resolvedGroup.label).toBe('Customer contact');
        expect(resolvedGroup.members).toHaveLength(1);
        expect(resolvedGroup.members[0]).toMatchObject({disabled: true, label: 'Customer contact'});
    });
});

describe('resolveBackingWorkflowNodeName', () => {
    const componentConnections = [
        {componentName: 'slack', workflowNodeName: 'slack_1'},
        {componentName: 'hubspot', workflowNodeName: 'hubspot_1'},
        {componentName: 'hubspot', workflowNodeName: 'hubspot_2'},
    ] as ComponentConnection[];

    it('returns the node of the first connection of the component that is configured', () => {
        expect(resolveBackingWorkflowNodeName('hubspot', componentConnections, [10, undefined, 30])).toBe('hubspot_2');
    });

    it('returns undefined when no connection of the component is configured', () => {
        expect(resolveBackingWorkflowNodeName('hubspot', componentConnections, [10])).toBeUndefined();
    });

    it('returns undefined without a component name or connections', () => {
        expect(resolveBackingWorkflowNodeName(undefined, componentConnections, [10, 20])).toBeUndefined();
        expect(resolveBackingWorkflowNodeName('hubspot', [], [10])).toBeUndefined();
        expect(resolveBackingWorkflowNodeName('hubspot', undefined, [10])).toBeUndefined();
    });
});
