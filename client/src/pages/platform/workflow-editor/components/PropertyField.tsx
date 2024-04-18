import {TYPE_ICONS} from '@/shared/typeIcons';
import {PropertyType} from '@/types/types';
import {ClipboardCheck, ClipboardIcon} from 'lucide-react';

import getNestedObject from '../utils/getNestedObject';

interface PropertyFieldProps {
    copiedValue: string | null;
    copyToClipboard: (text: string) => Promise<boolean>;
    label: string;
    property: PropertyType;
    parentPath?: string;
    sampleOutput: object;
    valueToCopy?: string;
    workflowNodeName: string;
}

const PropertyField = ({
    copiedValue,
    copyToClipboard,
    label = '[index]',
    parentPath,
    property,
    sampleOutput,
    valueToCopy,
    workflowNodeName,
}: PropertyFieldProps) => {
    const selector = `${parentPath ? parentPath + '.' : ''}${property.name || '[index]'}`.replace('/', '.');

    let value = getNestedObject(sampleOutput, selector);

    if (typeof value === 'string') {
        value = (value as string).substring(0, 35) + ((value as string).length > 35 ? '...' : '');
    }

    valueToCopy = valueToCopy || `$\{${workflowNodeName}.${selector}}`;

    return (
        <div>
            <div className="group inline-flex items-center rounded-md p-1 text-sm hover:bg-gray-100">
                <span title={property.type}>{TYPE_ICONS[property.type as keyof typeof TYPE_ICONS]}</span>

                <span className="px-2">{label}</span>

                {(value || value === 0 || value === false) && typeof value !== 'object' && (
                    <span className="flex-1 text-xs text-muted-foreground">{value}</span>
                )}

                {copiedValue === valueToCopy ? (
                    <ClipboardCheck className="mx-2 size-4 cursor-pointer text-green-600 hover:text-green-500" />
                ) : (
                    <ClipboardIcon
                        aria-hidden="true"
                        className="invisible mx-2 size-4 cursor-pointer text-gray-400 hover:text-gray-800 group-hover:visible"
                        onClick={() => copyToClipboard(valueToCopy!)}
                    />
                )}
            </div>
        </div>
    );
};

export default PropertyField;
