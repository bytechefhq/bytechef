import SubflowIcon from '@/assets/subflow.svg';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Label} from '@/components/ui/label';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {ComponentConnection} from '@/shared/middleware/automation/configuration';
import {
    ComponentDefinition,
    ComponentDefinitionApi,
    ControlType,
    PropertyType,
    WorkflowInput,
} from '@/shared/middleware/platform/configuration';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetWorkflowNodeOptionsQuery} from '@/shared/queries/platform/workflowNodeOptions.queries';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PropertyAllType} from '@/shared/types';
import {CaretDownIcon} from '@radix-ui/react-icons';
import {useQueries} from '@tanstack/react-query';
import {ArrowUpRightIcon, BoxIcon, CornerDownRightIcon, InfoIcon} from 'lucide-react';
import {useMemo} from 'react';
import {Control, Controller, FieldValues, FormState} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

import {SubflowDuplicateStubI} from './ConnectionConfigurationList';

const DANGLING_REFERENCE_MESSAGE = 'This input is no longer available.';

interface InputConfigurationListProps {
    componentConnections?: ComponentConnection[];
    configuredConnectionIds?: Array<number | undefined>;
    control: Control<FieldValues>;
    controlPath: string;
    duplicateSubflowStubs?: SubflowDuplicateStubI[];
    // Replaces the "Open Inputs" affordance when the caller knows that sheet cannot add an input — a code workflow's
    // inputs come from its source, so sending the user there would only repeat this message.
    emptyStateMessage?: string;
    formState: FormState<FieldValues>;
    inputs?: WorkflowInput[];
    onOpenInputs?: () => void;
    subflowLabelMap?: Map<string, string>;
    workflowId?: string;
}

interface SubflowInputTreeNodeI {
    children: Map<string, SubflowInputTreeNodeI>;
    duplicateStubs: SubflowDuplicateStubI[];
    inputs: PropertyAllType[];
    subflowWorkflowUuid: string;
}

const componentDefinitionKey = (componentName: string, componentVersion?: number): string =>
    `${componentName}:${componentVersion ?? ''}`;

const isComponentReferenceInput = (input: WorkflowInput): boolean => !!input.componentReference?.groupName;

const createDanglingPlaceholder = (input: WorkflowInput): PropertyAllType =>
    ({
        controlType: ControlType.Text,
        description: DANGLING_REFERENCE_MESSAGE,
        disabled: true,
        label: input.label || input.name,
        name: input.name,
        type: PropertyType.String,
    }) as PropertyAllType;

export interface ResolvedComponentInputGroupI {
    label?: string;
    members: PropertyAllType[];
    name: string;
}

/**
 * Resolves a component-referenced input group into its member properties (members keep their own names so they nest
 * under the group input). Every component-defined input is a group; a lone property is a group with one property.
 * Returns a single disabled TEXT placeholder member when the group is no longer available.
 */
export const resolveComponentInputGroup = (
    input: WorkflowInput,
    componentDefinition?: ComponentDefinition
): ResolvedComponentInputGroupI => {
    const group = componentDefinition?.inputs?.find((current) => current.name === input.componentReference?.groupName);

    if (!group) {
        return {
            label: input.label || input.name,
            members: [createDanglingPlaceholder(input)],
            name: input.name,
        };
    }

    const members = (group.properties ?? []).map((property) => property as PropertyAllType);

    return {
        label: input.label || group.label,
        members,
        name: input.name,
    };
};

/**
 * Finds the workflowNodeName of the first node (deterministically the first in document order) that uses the given
 * component and has a configured deployment connection, so component-referenced dynamic options can reuse that node's
 * connection context. Returns undefined when no such node exists; callers then degrade to free-text.
 */
export const resolveBackingWorkflowNodeName = (
    componentName: string | undefined,
    componentConnections?: ComponentConnection[],
    configuredConnectionIds?: Array<number | undefined>
): string | undefined => {
    if (!componentName || !componentConnections?.length) {
        return undefined;
    }

    for (let index = 0; index < componentConnections.length; index++) {
        const componentConnection = componentConnections[index];

        if (componentConnection.componentName === componentName && configuredConnectionIds?.[index] != null) {
            return componentConnection.workflowNodeName;
        }
    }

    return undefined;
};

const countSubflowNodeInputs = (node: SubflowInputTreeNodeI): number => {
    let count = node.inputs.length;

    for (const childNode of node.children.values()) {
        count += countSubflowNodeInputs(childNode);
    }

    return count;
};

const convertInputToProperty = (input: WorkflowInput): PropertyAllType => {
    switch (input.type) {
        case 'boolean':
            return {...input, controlType: ControlType.Select, type: PropertyType.Boolean} as PropertyAllType;
        case 'date':
            return {...input, controlType: ControlType.Date, type: PropertyType.Date} as PropertyAllType;
        case 'date_time':
            return {...input, controlType: ControlType.DateTime, type: PropertyType.DateTime} as PropertyAllType;
        case 'integer':
            return {...input, controlType: ControlType.Integer, type: PropertyType.Integer} as PropertyAllType;
        case 'number':
            return {...input, controlType: ControlType.Number, type: PropertyType.Number} as PropertyAllType;
        case 'string':
            return {...input, controlType: ControlType.Text, type: PropertyType.String} as PropertyAllType;
        default:
            return {...input, controlType: ControlType.Time, type: PropertyType.Time} as PropertyAllType;
    }
};

interface InheritedSubflowInputStubProps {
    stub: SubflowDuplicateStubI;
    subflowLabelMap?: Map<string, string>;
}

const InheritedSubflowInputStub = ({stub, subflowLabelMap}: InheritedSubflowInputStubProps) => (
    <div className="flex items-center gap-2 rounded-md border bg-surface-neutral-primary px-3 py-2.5">
        <InlineSVG className="size-5" src={SubflowIcon} />

        <span className="text-sm font-medium text-content-neutral-primary">
            {subflowLabelMap?.get(stub.subflowWorkflowUuid) || 'Subflow inputs'}
        </span>

        <Badge className="ml-auto flex gap-1 font-semibold uppercase" styleType="secondary-filled">
            <CornerDownRightIcon className="size-3" />
            Inherited
        </Badge>
    </div>
);

interface DanglingReferenceInputProps {
    property: PropertyAllType;
}

const DanglingReferenceInput = ({property}: DanglingReferenceInputProps) => (
    <fieldset className="w-full space-y-1 border-0 p-0">
        <Label className="text-sm font-medium text-content-neutral-primary">{property.label}</Label>

        <input
            className="flex h-9 w-full cursor-not-allowed rounded-md border border-stroke-neutral-secondary bg-surface-neutral-secondary px-3 py-1 text-sm text-content-neutral-secondary"
            disabled
            placeholder={DANGLING_REFERENCE_MESSAGE}
            type="text"
        />

        <p className="flex items-center gap-1 text-sm font-light text-content-warning-primary">
            <InfoIcon className="size-4" /> {DANGLING_REFERENCE_MESSAGE}
        </p>
    </fieldset>
);

interface ComponentReferenceDynamicSelectProps {
    control: Control<FieldValues>;
    controlPath: string;
    environmentId: number;
    property: PropertyAllType;
    valuePropertyName?: string;
    workflowId: string;
    workflowNodeName: string;
}

const ComponentReferenceDynamicSelect = ({
    control,
    controlPath,
    environmentId,
    property,
    valuePropertyName,
    workflowId,
    workflowNodeName,
}: ComponentReferenceDynamicSelectProps) => {
    const {data: options, isLoading} = useGetWorkflowNodeOptionsQuery(
        {
            loadDependencyValueKey: '',
            request: {
                environmentId,
                id: workflowId,
                propertyName: property.name!,
                workflowNodeName,
            },
        },
        Boolean(workflowId && workflowNodeName)
    );

    return (
        <fieldset className="w-full space-y-1 border-0 p-0">
            <Label className="text-sm font-medium text-content-neutral-primary">{property.label}</Label>

            <Controller
                control={control}
                name={`${controlPath}.${valuePropertyName ?? property.name}`}
                render={({field}) => (
                    <select
                        className="flex h-9 w-full rounded-md border border-stroke-neutral-secondary bg-surface-neutral-primary px-3 py-1 text-sm text-content-neutral-primary"
                        onChange={(event) => field.onChange(event.target.value)}
                        value={field.value ?? ''}
                    >
                        <option disabled value="">
                            {isLoading ? 'Loading...' : 'Select...'}
                        </option>

                        {(options ?? []).map((option) => (
                            <option key={String(option.value)} value={String(option.value)}>
                                {option.label ?? String(option.value)}
                            </option>
                        ))}
                    </select>
                )}
            />

            {property.description && (
                <p className="text-sm font-light text-content-neutral-secondary">{property.description}</p>
            )}
        </fieldset>
    );
};

interface ComponentReferenceMemberProps {
    backingWorkflowNodeName?: string;
    control: Control<FieldValues>;
    controlPath: string;
    formState: FormState<FieldValues>;
    property: PropertyAllType;
    // When set, the value binds to `${controlPath}.${valuePropertyName}` instead of the property's own
    // name. A lone-property input passes the input name so its value is flat (`${input.<name>}`), while
    // dynamic options still resolve against the original component property name.
    valuePropertyName?: string;
    workflowId?: string;
}

const ComponentReferenceMember = ({
    backingWorkflowNodeName,
    control,
    controlPath,
    formState,
    property,
    valuePropertyName,
    workflowId,
}: ComponentReferenceMemberProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    if (property.disabled) {
        return <DanglingReferenceInput property={property} />;
    }

    const hasDynamicOptions = !!property.optionsDataSource;

    if (hasDynamicOptions && backingWorkflowNodeName && workflowId) {
        return (
            <ComponentReferenceDynamicSelect
                control={control}
                controlPath={controlPath}
                environmentId={currentEnvironmentId}
                property={property}
                valuePropertyName={valuePropertyName}
                workflowId={workflowId}
                workflowNodeName={backingWorkflowNodeName}
            />
        );
    }

    const valueProperty = valuePropertyName ? ({...property, name: valuePropertyName} as PropertyAllType) : property;

    return (
        <fieldset className="w-full space-y-1 border-0 p-0">
            <Properties
                control={control}
                controlPath={controlPath}
                formState={formState}
                properties={[valueProperty]}
            />

            {hasDynamicOptions && !backingWorkflowNodeName && (
                <p className="flex items-center gap-1 text-sm font-light text-content-neutral-secondary">
                    <InfoIcon className="size-4" /> Configure a connection for this component to load live options.
                </p>
            )}
        </fieldset>
    );
};

interface ComponentReferenceGroupProps {
    backingWorkflowNodeName?: string;
    control: Control<FieldValues>;
    controlPath: string;
    formState: FormState<FieldValues>;
    group: ResolvedComponentInputGroupI;
    workflowId?: string;
}

const ComponentReferenceGroup = ({
    backingWorkflowNodeName,
    control,
    controlPath,
    formState,
    group,
    workflowId,
}: ComponentReferenceGroupProps) => {
    const memberCount = group.members.length;

    return (
        <Collapsible
            className="group/group space-y-4 rounded-md border bg-surface-neutral-primary px-3 py-2.5 transition-all has-[>button:focus-visible]:ring-2 has-[>button:focus-visible]:ring-stroke-brand-focus data-[state=open]:p-3"
            defaultOpen
        >
            <CollapsibleTrigger className="group/trigger flex w-full items-center justify-between outline-hidden">
                <div className="flex gap-2">
                    <BoxIcon className="size-5 text-content-neutral-secondary" />

                    <span className="text-sm font-medium text-content-neutral-primary underline-offset-2 group-hover/trigger:underline">
                        {group.label || group.name}
                    </span>

                    <span className="text-sm font-light text-content-neutral-primary">
                        ({memberCount > 1 ? `${memberCount} inputs` : '1 input'})
                    </span>
                </div>

                <CaretDownIcon className="size-4 text-content-neutral-secondary transition-all group-data-[state=open]/group:rotate-180" />
            </CollapsibleTrigger>

            <CollapsibleContent className="flex flex-col gap-4">
                {group.members.map((member) => (
                    <ComponentReferenceMember
                        backingWorkflowNodeName={backingWorkflowNodeName}
                        control={control}
                        controlPath={`${controlPath}.${group.name}`}
                        formState={formState}
                        key={member.name}
                        property={member}
                        workflowId={workflowId}
                    />
                ))}
            </CollapsibleContent>
        </Collapsible>
    );
};

interface ComponentReferenceInputProps {
    componentConnections?: ComponentConnection[];
    componentDefinition?: ComponentDefinition;
    configuredConnectionIds?: Array<number | undefined>;
    control: Control<FieldValues>;
    controlPath: string;
    formState: FormState<FieldValues>;
    input: WorkflowInput;
    isLoadingComponentDefinition?: boolean;
    workflowId?: string;
}

const ComponentReferenceInput = ({
    componentConnections,
    componentDefinition,
    configuredConnectionIds,
    control,
    controlPath,
    formState,
    input,
    isLoadingComponentDefinition,
    workflowId,
}: ComponentReferenceInputProps) => {
    const backingWorkflowNodeName = resolveBackingWorkflowNodeName(
        input.componentReference?.componentName,
        componentConnections,
        configuredConnectionIds
    );

    if (isLoadingComponentDefinition) {
        return (
            <Properties
                control={control}
                controlPath={controlPath}
                formState={formState}
                properties={[
                    {
                        controlType: ControlType.Text,
                        label: input.label || input.name,
                        name: input.name,
                        type: PropertyType.String,
                    } as PropertyAllType,
                ]}
            />
        );
    }

    const group = resolveComponentInputGroup(input, componentDefinition);

    // A lone-property input (a group with a single property) binds its value flat under the input name
    // (`${input.<name>}`) rather than nesting it under the group, so the value is the property's value
    // directly — what an action's string parameter expects.
    if (group.members.length === 1) {
        return (
            <ComponentReferenceMember
                backingWorkflowNodeName={backingWorkflowNodeName}
                control={control}
                controlPath={controlPath}
                formState={formState}
                property={group.members[0]}
                valuePropertyName={input.name}
                workflowId={workflowId}
            />
        );
    }

    return (
        <ComponentReferenceGroup
            backingWorkflowNodeName={backingWorkflowNodeName}
            control={control}
            controlPath={controlPath}
            formState={formState}
            group={group}
            workflowId={workflowId}
        />
    );
};

interface ComponentReferenceInputListProps {
    componentConnections?: ComponentConnection[];
    configuredConnectionIds?: Array<number | undefined>;
    control: Control<FieldValues>;
    controlPath: string;
    formState: FormState<FieldValues>;
    referenceInputs: WorkflowInput[];
    workflowId?: string;
}

/**
 * Fetches the (few) distinct component definitions referenced by the reference inputs and renders each reference.
 * Definitions are fetched with a single useQueries call keyed on the distinct {componentName, componentVersion} set so
 * React's hook rules are respected even though the number of distinct components is data-driven. While a definition is
 * loading each reference degrades to its free-text placeholder rather than crashing.
 */
const ComponentReferenceInputList = ({
    componentConnections,
    configuredConnectionIds,
    control,
    controlPath,
    formState,
    referenceInputs,
    workflowId,
}: ComponentReferenceInputListProps) => {
    const distinctComponents = useMemo(() => {
        const seen = new Map<string, {componentName: string; componentVersion?: number}>();

        for (const input of referenceInputs) {
            const componentReference = input.componentReference;

            if (!componentReference) {
                continue;
            }

            const key = componentDefinitionKey(componentReference.componentName, componentReference.componentVersion);

            if (!seen.has(key)) {
                seen.set(key, {
                    componentName: componentReference.componentName,
                    componentVersion: componentReference.componentVersion,
                });
            }
        }

        return Array.from(seen.values());
    }, [referenceInputs]);

    const componentDefinitionResults = useQueries({
        queries: distinctComponents.map((component) => {
            const request = {
                componentName: component.componentName,
                componentVersion: component.componentVersion ?? 1,
            };

            return {
                queryFn: () => new ComponentDefinitionApi().getComponentDefinition(request),
                queryKey: ComponentDefinitionKeys.componentDefinition(request),
                staleTime: DEFINITION_STALE_TIME,
            };
        }),
    });

    const {componentDefinitionMap, loadingKeys} = useMemo(() => {
        const definitionMap = new Map<string, ComponentDefinition>();
        const pendingKeys = new Set<string>();

        distinctComponents.forEach((component, index) => {
            const key = componentDefinitionKey(component.componentName, component.componentVersion);
            const queryResult = componentDefinitionResults[index];

            if (queryResult?.data) {
                definitionMap.set(key, queryResult.data);
            } else if (queryResult?.isLoading) {
                pendingKeys.add(key);
            }
        });

        return {componentDefinitionMap: definitionMap, loadingKeys: pendingKeys};
    }, [distinctComponents, componentDefinitionResults]);

    return (
        <div className="space-y-4">
            {referenceInputs.map((input) => {
                const key = componentDefinitionKey(
                    input.componentReference!.componentName,
                    input.componentReference!.componentVersion
                );

                return (
                    <ComponentReferenceInput
                        componentConnections={componentConnections}
                        componentDefinition={componentDefinitionMap.get(key)}
                        configuredConnectionIds={configuredConnectionIds}
                        control={control}
                        controlPath={controlPath}
                        formState={formState}
                        input={input}
                        isLoadingComponentDefinition={loadingKeys.has(key)}
                        key={input.name}
                        workflowId={workflowId}
                    />
                );
            })}
        </div>
    );
};

interface SubflowInputGroupProps {
    control: Control<FieldValues>;
    controlPath: string;
    formState: FormState<FieldValues>;
    inputTreeNode: SubflowInputTreeNodeI;
    subflowLabelMap?: Map<string, string>;
}

const SubflowInputGroup = ({
    control,
    controlPath,
    formState,
    inputTreeNode,
    subflowLabelMap,
}: SubflowInputGroupProps) => {
    const inputCount = countSubflowNodeInputs(inputTreeNode);

    return (
        <Collapsible
            className="group/subflow space-y-4 rounded-md border bg-surface-neutral-primary px-3 py-2.5 transition-all has-[>button:focus-visible]:ring-2 has-[>button:focus-visible]:ring-stroke-brand-focus data-[state=open]:p-3"
            defaultOpen
        >
            <CollapsibleTrigger className="group/trigger flex w-full items-center justify-between outline-hidden">
                <div className="flex gap-2">
                    <InlineSVG className="size-5" src={SubflowIcon} />

                    <span className="text-sm font-medium text-content-neutral-primary underline-offset-2 group-hover/trigger:underline">
                        {subflowLabelMap?.get(inputTreeNode.subflowWorkflowUuid) || 'Subflow inputs'}
                    </span>

                    <span className="text-sm font-light text-content-neutral-primary">
                        ({inputCount > 1 ? `${inputCount} inputs` : '1 input'})
                    </span>
                </div>

                <CaretDownIcon className="size-4 text-content-neutral-secondary transition-all group-data-[state=open]/subflow:rotate-180" />
            </CollapsibleTrigger>

            <CollapsibleContent className="flex flex-col gap-4">
                {!!inputTreeNode.inputs.length && (
                    <Properties
                        control={control}
                        controlPath={controlPath}
                        formState={formState}
                        properties={inputTreeNode.inputs}
                    />
                )}

                {Array.from(inputTreeNode.children.values()).map((childInputNode) => (
                    <SubflowInputGroup
                        control={control}
                        controlPath={controlPath}
                        formState={formState}
                        inputTreeNode={childInputNode}
                        key={childInputNode.subflowWorkflowUuid}
                        subflowLabelMap={subflowLabelMap}
                    />
                ))}

                {inputTreeNode.duplicateStubs.map((inputStub) => (
                    <InheritedSubflowInputStub
                        key={inputStub.subflowWorkflowUuidPath.join('/')}
                        stub={inputStub}
                        subflowLabelMap={subflowLabelMap}
                    />
                ))}
            </CollapsibleContent>
        </Collapsible>
    );
};

const InputConfigurationList = ({
    componentConnections,
    configuredConnectionIds,
    control,
    controlPath,
    duplicateSubflowStubs,
    emptyStateMessage,
    formState,
    inputs,
    onOpenInputs,
    subflowLabelMap,
    workflowId,
}: InputConfigurationListProps) => {
    const {referenceInputs, regularInputs, subflowInputTree, topLevelStubs} = useMemo(() => {
        const referenceInputs: WorkflowInput[] = [];
        const regularInputs: PropertyAllType[] = [];
        const subflowInputTree = new Map<string, SubflowInputTreeNodeI>();

        const createNode = (uuid: string): SubflowInputTreeNodeI => ({
            children: new Map<string, SubflowInputTreeNodeI>(),
            duplicateStubs: [],
            inputs: [],
            subflowWorkflowUuid: uuid,
        });

        for (const input of inputs ?? []) {
            const uuidPath = input.subflowWorkflowUuidPath ?? [];

            if (!uuidPath.length && isComponentReferenceInput(input)) {
                referenceInputs.push(input);

                continue;
            }

            const property = convertInputToProperty(input);

            if (!uuidPath.length) {
                regularInputs.push(property);

                continue;
            }

            let currentLevel = subflowInputTree;
            let targetNode: SubflowInputTreeNodeI | undefined;

            for (const uuid of uuidPath) {
                let node = currentLevel.get(uuid);

                if (!node) {
                    node = createNode(uuid);

                    currentLevel.set(uuid, node);
                }

                targetNode = node;
                currentLevel = node.children;
            }

            targetNode!.inputs.push(property);
        }

        const topLevelStubs: SubflowDuplicateStubI[] = [];

        for (const stub of duplicateSubflowStubs ?? []) {
            const parentPath = stub.subflowWorkflowUuidPath.slice(0, -1);

            if (!parentPath.length) {
                topLevelStubs.push(stub);

                continue;
            }

            let currentLevel = subflowInputTree;
            let parentNode: SubflowInputTreeNodeI | undefined;

            for (const uuid of parentPath) {
                let node = currentLevel.get(uuid);

                if (!node) {
                    node = createNode(uuid);

                    currentLevel.set(uuid, node);
                }

                parentNode = node;
                currentLevel = node.children;
            }

            parentNode!.duplicateStubs.push(stub);
        }

        return {referenceInputs, regularInputs, subflowInputTree, topLevelStubs};
    }, [inputs, duplicateSubflowStubs]);

    if (!referenceInputs.length && !regularInputs.length && !subflowInputTree.size && !topLevelStubs.length) {
        if (emptyStateMessage) {
            return (
                <div className="flex flex-col items-center gap-2">
                    <h3 className="font-medium text-content-neutral-primary">No Inputs yet</h3>

                    <p className="text-sm text-content-neutral-secondary">{emptyStateMessage}</p>
                </div>
            );
        }

        if (!onOpenInputs) {
            return <p className="text-sm">No Inputs yet</p>;
        }

        return (
            <div className="flex flex-col items-center gap-4">
                <h3 className="font-medium text-content-neutral-primary">No Inputs yet</h3>

                <div className="flex flex-col items-center gap-2">
                    <Button onClick={onOpenInputs} variant="outline">
                        <ArrowUpRightIcon />
                        Open Inputs
                    </Button>

                    <p className="flex items-center justify-center gap-1 text-sm font-light text-content-warning-primary">
                        <InfoIcon className="size-4" /> This will discard the current configuration.
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-4">
            {!!regularInputs.length && (
                <Properties
                    control={control}
                    controlPath={controlPath}
                    formState={formState}
                    properties={regularInputs}
                />
            )}

            {!!referenceInputs.length && (
                <ComponentReferenceInputList
                    componentConnections={componentConnections}
                    configuredConnectionIds={configuredConnectionIds}
                    control={control}
                    controlPath={controlPath}
                    formState={formState}
                    referenceInputs={referenceInputs}
                    workflowId={workflowId}
                />
            )}

            {Array.from(subflowInputTree.values()).map((inputNode) => (
                <SubflowInputGroup
                    control={control}
                    controlPath={controlPath}
                    formState={formState}
                    inputTreeNode={inputNode}
                    key={inputNode.subflowWorkflowUuid}
                    subflowLabelMap={subflowLabelMap}
                />
            ))}

            {topLevelStubs.map((inputStub) => (
                <InheritedSubflowInputStub
                    key={inputStub.subflowWorkflowUuidPath.join('/')}
                    stub={inputStub}
                    subflowLabelMap={subflowLabelMap}
                />
            ))}
        </div>
    );
};

export default InputConfigurationList;
