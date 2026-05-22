import SubflowIcon from '@/assets/subflow.svg';
import Button from '@/components/Button/Button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {ControlType, PropertyType, WorkflowInput} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';
import {CaretDownIcon} from '@radix-ui/react-icons';
import {ArrowUpRightIcon, InfoIcon} from 'lucide-react';
import {useMemo} from 'react';
import {Control, FieldValues, FormState} from 'react-hook-form';
import InlineSVG from 'react-inlinesvg';

interface InputConfigurationListProps {
    control: Control<FieldValues>;
    controlPath: string;
    formState: FormState<FieldValues>;
    inputs?: WorkflowInput[];
    onOpenInputs?: () => void;
    subflowLabelMap?: Map<string, string>;
}

interface SubflowInputTreeNodeI {
    children: Map<string, SubflowInputTreeNodeI>;
    inputs: PropertyAllType[];
    subflowWorkflowUuid: string;
}

const toProperty = (input: WorkflowInput): PropertyAllType => {
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

interface SubflowInputGroupProps {
    control: Control<FieldValues>;
    controlPath: string;
    formState: FormState<FieldValues>;
    node: SubflowInputTreeNodeI;
    subflowLabelMap?: Map<string, string>;
}

const SubflowInputGroup = ({control, controlPath, formState, node, subflowLabelMap}: SubflowInputGroupProps) => {
    const inputCount = countSubflowNodeInputs(node);

    return (
        <Collapsible className="group/subflow space-y-4 rounded-md border bg-surface-neutral-primary px-3 py-2.5 transition-all has-[>button:focus-visible]:ring-2 has-[>button:focus-visible]:ring-stroke-brand-focus data-[state=open]:border-stroke-brand-primary data-[state=open]:p-3">
            <CollapsibleTrigger className="group/trigger flex w-full items-center justify-between outline-none">
                <div className="flex gap-2">
                    <InlineSVG className="size-5" src={SubflowIcon} />

                    <span className="text-sm font-medium text-content-neutral-primary underline-offset-2 group-hover/trigger:underline">
                        {subflowLabelMap?.get(node.subflowWorkflowUuid) || 'Subflow inputs'}
                    </span>

                    <span className="text-sm font-light text-content-neutral-primary">
                        ({inputCount > 1 ? `${inputCount} inputs` : '1 input'})
                    </span>
                </div>

                <CaretDownIcon className="size-4 text-content-neutral-secondary transition-all group-data-[state=open]/subflow:rotate-180" />
            </CollapsibleTrigger>

            <CollapsibleContent className="flex flex-col gap-4">
                {!!node.inputs.length && (
                    <Properties
                        control={control}
                        controlPath={controlPath}
                        formState={formState}
                        properties={node.inputs}
                    />
                )}

                {Array.from(node.children.values()).map((childNode) => (
                    <SubflowInputGroup
                        control={control}
                        controlPath={controlPath}
                        formState={formState}
                        key={childNode.subflowWorkflowUuid}
                        node={childNode}
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
    formState,
    inputs,
    onOpenInputs,
    subflowLabelMap,
}: InputConfigurationListProps) => {
    const {regularInputs, subflowInputTree} = useMemo(() => {
        const regularInputs: PropertyAllType[] = [];
        const subflowInputTree = new Map<string, SubflowInputTreeNodeI>();

        for (const input of inputs ?? []) {
            const uuidPath = input.subflowWorkflowUuidPath ?? [];
            const property = toProperty(input);

            if (!uuidPath.length) {
                regularInputs.push(property);

                continue;
            }

            let currentLevel = subflowInputTree;
            let targetNode: SubflowInputTreeNodeI | undefined;

            for (const uuid of uuidPath) {
                let node = currentLevel.get(uuid);

                if (!node) {
                    node = {children: new Map<string, SubflowInputTreeNodeI>(), inputs: [], subflowWorkflowUuid: uuid};

                    currentLevel.set(uuid, node);
                }

                targetNode = node;
                currentLevel = node.children;
            }

            targetNode!.inputs.push(property);
        }

        return {regularInputs, subflowInputTree};
    }, [inputs]);

    if (!regularInputs.length && !subflowInputTree.size) {
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

            {Array.from(subflowInputTree.values()).map((node) => (
                <SubflowInputGroup
                    control={control}
                    controlPath={controlPath}
                    formState={formState}
                    key={node.subflowWorkflowUuid}
                    node={node}
                    subflowLabelMap={subflowLabelMap}
                />
            ))}
        </div>
    );
};

export default InputConfigurationList;
