import {DataPillType} from '@/shared/types';

import {toArrayIndexTemplate} from './dataPillArrayIndex';

export function buildValidDataPillReferenceSet(dataPills: Array<DataPillType>): Set<string> {
    const validReferences = new Set<string>();

    for (const dataPill of dataPills) {
        const value = dataPill.value;

        validReferences.add(value);

        const arrayIndexTemplate = toArrayIndexTemplate(value);

        validReferences.add(arrayIndexTemplate);
    }

    return validReferences;
}

export function isDataPillReferenceValid(reference: string | null | undefined, validReferences: Set<string>): boolean {
    if (!reference || validReferences.size === 0) {
        return true;
    }

    if (validReferences.has(reference)) {
        return true;
    }

    const arrayIndexTemplate = toArrayIndexTemplate(reference);

    return validReferences.has(arrayIndexTemplate);
}
