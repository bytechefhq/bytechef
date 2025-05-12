import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {PropertyAllType} from '@/shared/types';
import {ClipboardCheck, ClipboardIcon, InfoIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import getNestedObject from '../utils/getNestedObject';

interface PropertyFieldProps {
    copiedValue: string | null;
    copyToClipboard: (text: string) => Promise<void>;
    label: string;
    property: PropertyAllType;
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

    valueToCopy = valueToCopy || `$\{${workflowNodeName}.${selector}}`;

    return (
        <div className="group inline-flex w-full items-center justify-between rounded-md p-1 text-sm hover:bg-surface-neutral-primary-hover">
            <div className="flex w-11/12 items-center gap-2">
                {label !== '[index]' && (
                    <span title={property.type}>{TYPE_ICONS[property.type as keyof typeof TYPE_ICONS]}</span>
                )}

                {label === '[index]' && <span title={property.type}>{TYPE_ICONS.INTEGER}</span>}

                <span>{label}</span>

                {sampleValue !== undefined && typeof sampleValue !== 'object' && (
                    <span
                        className="flex-1 truncate text-xs text-content-neutral-secondary"
                        title={String(sampleValue)}
                    >
                        {String(sampleValue)}
                    </span>
                )}
            </div>

            <div className="flex items-center">
                {copiedValue === valueToCopy ? (
                    <ClipboardCheck className="mx-2 size-4 cursor-pointer text-success" />
                ) : (
                    <ClipboardIcon
                        aria-hidden="true"
                        className="invisible mx-2 size-4 cursor-pointer text-content-neutral-secondary hover:text-content-neutral-primary group-hover:visible"
                        onClick={() => copyToClipboard(valueToCopy!)}
                    />
                )}

                <Tooltip>
                    <TooltipTrigger>
                        <div
                            className={twMerge(
                                'cursor-auto rounded-md p-0.5',
                                property.description && 'cursor-pointer hover:bg-surface-neutral-tertiary'
                            )}
                        >
                            <InfoIcon
                                className={twMerge(
                                    'invisible size-4 text-content-neutral-secondary',
                                    property.description && 'group-hover:visible'
                                )}
                            />
                        </div>
                    </TooltipTrigger>

                    {property.description && (
                        <TooltipContent className="mr-2 max-w-72 whitespace-normal break-normal">
                            <span className="block">{property.description}</span>
                        </TooltipContent>
                    )}
                </Tooltip>
            </div>
        </div>
    );
};

export default PropertyField;
