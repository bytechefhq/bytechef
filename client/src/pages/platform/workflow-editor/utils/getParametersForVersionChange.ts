import {PropertyAllType} from '@/shared/types';

import getParametersWithDefaultValues from './getParametersWithDefaultValues';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type ParametersType = {[key: string]: any};

const isPlainObject = (value: unknown): value is ParametersType =>
    typeof value === 'object' && value !== null && !Array.isArray(value);

export default function getParametersForVersionChange({
    currentParameters,
    properties,
}: {
    currentParameters?: ParametersType;
    properties: Array<PropertyAllType>;
}): ParametersType {
    const parameters = getParametersWithDefaultValues({data: {}, properties}) as ParametersType;

    if (!currentParameters) {
        return parameters;
    }

    properties.forEach((property) => {
        if (!property.name || !(property.name in currentParameters)) {
            return;
        }

        const currentValue = currentParameters[property.name];

        if (property.properties?.length && isPlainObject(currentValue)) {
            parameters[property.name] = getParametersForVersionChange({
                currentParameters: currentValue,
                properties: property.properties as Array<PropertyAllType>,
            });

            return;
        }

        parameters[property.name] = currentValue;
    });

    return parameters;
}
