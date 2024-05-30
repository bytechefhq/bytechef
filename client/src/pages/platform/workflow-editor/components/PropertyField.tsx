import {TYPE_ICONS} from '@/shared/typeIcons';
import {PropertyType} from '@/shared/types';
import {ClipboardCheck, ClipboardIcon} from 'lucide-react';

import getNestedObject from '../utils/getNestedObject';

interface PropertyFieldProps {
    copiedValue: string | null;
    copyToClipboard: (text: string) => Promise<void>;
    label: string;
    property: PropertyType;
    parentPath?: string;
    /* eslint-disable  @typescript-eslint/no-explicit-any */
    sampleOutput: any;
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

    let sampleValue;

    if (typeof sampleOutput === 'object') {
        sampleValue = getNestedObject(sampleOutput, selector);
    } else {
        sampleValue = sampleOutput;
    }

    if (typeof sampleValue === 'string') {
        sampleValue = (sampleValue as string).substring(0, 35) + ((sampleValue as string).length > 35 ? '...' : '');
    }

    valueToCopy = valueToCopy || `$\{${workflowNodeName}.${selector}}`;

    return (
        <div>
            <div className="group inline-flex items-center rounded-md p-1 text-sm hover:bg-gray-100">
                {label !== '[index]' && (
                    <span title={property.type}>{TYPE_ICONS[property.type as keyof typeof TYPE_ICONS]}</span>
                )}

                {label === '[index]' && <span title={property.type}>{TYPE_ICONS.INTEGER}</span>}

                <span className="px-2">{label}</span>

                {(sampleValue || sampleValue === 0 || sampleValue === false) && typeof sampleValue !== 'object' && (
                    <span className="flex-1 text-xs text-muted-foreground">
                        {sampleValue === true ? 'true' : sampleValue === false ? false : sampleValue}
                    </span>
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
