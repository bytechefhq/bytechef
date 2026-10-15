import {NodeDataType} from '@/shared/types';

export default function collectClusterElementsSignature(value: unknown): string {
    if (Array.isArray(value)) {
        return value.map((item) => collectClusterElementsSignature(item)).join(',');
    }

    if (!value || typeof value !== 'object') {
        return '';
    }

    const record = value as Record<string, unknown>;

    if (typeof record.name === 'string' && typeof record.type === 'string') {
        const nodePosition = (record.metadata as NodeDataType['metadata'])?.ui?.nodePosition;
        const positionSignature = nodePosition ? `${nodePosition.x},${nodePosition.y}` : '';
        const nestedSignature = collectClusterElementsSignature(record.clusterElements);
        const labelSignature = typeof record.label === 'string' ? record.label : '';
        const nestedPart = nestedSignature ? '[' + nestedSignature + ']' : '';

        return `${record.name}:${record.type}:${labelSignature}@${positionSignature}${nestedPart}`;
    }

    return Object.keys(record)
        .sort((firstKey, secondKey) => firstKey.localeCompare(secondKey))
        .map((key) => `${key}=${collectClusterElementsSignature(record[key])}`)
        .join('|');
}
