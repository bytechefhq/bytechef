import SubflowIcon from '@/assets/subflow.svg';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {ControlType, PropertyType, WorkflowInput} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';
import {CaretDownIcon} from '@radix-ui/react-icons';
import {ArrowUpRightIcon, CornerDownRightIcon, InfoIcon} from 'lucide-react';
import {useMemo} from 'react';
import {Control, FieldValues, FormState} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

import {SubflowDuplicateStubI} from './ConnectionConfigurationList';

interface InputConfigurationListProps {
    control: Control<FieldValues>;
    controlPath: string;
    duplicateSubflowStubs?: SubflowDuplicateStubI[];
    formState: FormState<FieldValues>;
    inputs?: WorkflowInput[];
    onOpenInputs?: () => void;
    subflowLabelMap?: Map<string, string>;
}

interface SubflowInputTreeNodeI {
    children: Map<string, SubflowInputTreeNodeI>;
    duplicateStubs: SubflowDuplicateStubI[];
    inputs: PropertyAllType[];
    subflowWorkflowUuid: string;
}

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

const countSubflowNodeInputs = (node: SubflowInputTreeNodeI): number => {
    let count = node.inputs.length;

    for (const childNode of node.children.values()) {
        count += countSubflowNodeInputs(childNode);
    }

    return count;
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
    control,
    controlPath,
    duplicateSubflowStubs,
    formState,
    inputs,
    onOpenInputs,
    subflowLabelMap,
}: InputConfigurationListProps) => {
    const {regularInputs, subflowInputTree, topLevelStubs} = useMemo(() => {
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

        return {regularInputs, subflowInputTree, topLevelStubs};
    }, [inputs, duplicateSubflowStubs]);

    if (!regularInputs.length && !subflowInputTree.size && !topLevelStubs.length) {
        return (
            <div className="flex flex-col items-center gap-4">
                <h3 className="font-medium text-content-neutral-primary">No Inputs yet</h3>

                {onOpenInputs && (
                    <div className="flex flex-col items-center gap-2">
                        <Button onClick={onOpenInputs} variant="outline">
                            <ArrowUpRightIcon />
                            Open Inputs
                        </Button>

                        <p className="flex items-center justify-center gap-1 text-sm font-light text-content-warning-primary">
                            <InfoIcon className="size-4" /> This will discard the current configuration.
                        </p>
                    </div>
                )}
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
