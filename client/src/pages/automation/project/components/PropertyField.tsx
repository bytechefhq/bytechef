import useCopyToClipboard from '@/hooks/useCopyToClipboard';
import {TYPE_ICONS} from '@/shared/typeIcons';
import {PropertyType} from '@/types/types';
import {ClipboardIcon} from 'lucide-react';

import getNestedObject from '../utils/getNestedObject';

const PropertyField = ({
    label = '[index]',
    parentPath,
    property,
    sampleOutput,
    workflowNodeName,
}: {
    label: string;
    property: PropertyType;
    parentPath?: string;
    sampleOutput: object;
    workflowNodeName: string;
}) => {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const [_, copyToClipboard] = useCopyToClipboard();

    const selector = `${parentPath ? parentPath + '.' : ''}${property.name}`.replace('/', '.');

    const value = property.name && getNestedObject(sampleOutput, selector);

    return (
        <div>
            <div className="group inline-flex items-center rounded-md p-1 text-sm hover:bg-gray-100">
                <span title={property.type}>{TYPE_ICONS[property.type as keyof typeof TYPE_ICONS]}</span>

                <span className="px-2">{label}</span>

                {(value || value === 0 || value === false) && typeof value !== 'object' && (
                    <span className="flex-1 text-xs text-muted-foreground">
                        {value === true ? 'true' : value === false ? false : value}
                    </span>
                )}

                <ClipboardIcon
                    aria-hidden="true"
                    className="invisible mx-2 size-4 cursor-pointer text-gray-400 hover:text-gray-800 group-hover:visible"
                    onClick={() => copyToClipboard(`$\{${workflowNodeName}.${selector}}`)}
                />
            </div>
        </div>
    );
};

export default PropertyField;
