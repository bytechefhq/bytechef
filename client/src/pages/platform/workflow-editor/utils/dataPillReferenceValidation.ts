import {DataPillType} from '@/shared/types';

export function buildValidDataPillReferenceSet(dataPills: Array<DataPillType>): Set<string> {
    const set = new Set<string>();

    for (const dataPill of dataPills) {
        const value = dataPill.value;

        set.add(value);

        if (value.includes('[index]')) {
            set.add(value.replaceAll('[index]', '[0]'));
        }

        if (value.includes('[0]')) {
            set.add(value.replaceAll('[0]', '[index]'));
        }
    }

    return set;
}

export function isDataPillReferenceValid(reference: string | null | undefined, validSet: Set<string>): boolean {
    if (!reference || validSet.size === 0) {
        return true;
    }

    if (validSet.has(reference)) {
        return true;
    }

    if (reference.includes('[index]')) {
        const withZero = reference.replaceAll('[index]', '[0]');

        if (validSet.has(withZero)) {
            return true;
        }
    }

    if (reference.includes('[0]')) {
        const withIndex = reference.replaceAll('[0]', '[index]');

        if (validSet.has(withIndex)) {
            return true;
        }
    }

    return false;
}
