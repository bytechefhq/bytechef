import Button from '@/components/Button/Button';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {ControlType, PropertyType, WorkflowInput} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';
import {ArrowUpRightIcon, InfoIcon} from 'lucide-react';
import {Control, FieldValues, FormState} from 'react-hook-form';

interface InputConfigurationListProps {
    control: Control<FieldValues>;
    controlPath: string;
    formState: FormState<FieldValues>;
    inputs?: WorkflowInput[];
    onOpenInputs?: () => void;
}

const InputConfigurationList = ({
    control,
    controlPath,
    formState,
    inputs,
    onOpenInputs,
}: InputConfigurationListProps) => {
    const properties = inputs?.map((input) => {
        switch (input.type) {
            case 'boolean':
                return {
                    ...input,
                    controlType: ControlType.Select,
                    type: PropertyType.Boolean,
                } as PropertyAllType;
            case 'date':
                return {
                    ...input,
                    controlType: ControlType.Date,
                    type: PropertyType.Date,
                } as PropertyAllType;
            case 'date_time':
                return {
                    ...input,
                    controlType: ControlType.DateTime,
                    type: PropertyType.DateTime,
                } as PropertyAllType;
            case 'integer':
                return {
                    ...input,
                    controlType: ControlType.Integer,
                    type: PropertyType.Integer,
                } as PropertyAllType;
            case 'number':
                return {
                    ...input,
                    controlType: ControlType.Number,
                    type: PropertyType.Number,
                } as PropertyAllType;
            case 'string':
                return {
                    ...input,
                    controlType: ControlType.Text,
                    type: PropertyType.String,
                } as PropertyAllType;
            default:
                return {
                    ...input,
                    controlType: ControlType.Time,
                    type: PropertyType.Time,
                } as PropertyAllType;
        }
    });

    if (!properties?.length) {
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

    return <Properties control={control} controlPath={controlPath} formState={formState} properties={properties} />;
};

export default InputConfigurationList;
