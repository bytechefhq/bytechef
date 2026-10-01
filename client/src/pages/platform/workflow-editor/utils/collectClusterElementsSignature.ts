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

        return `${record.name}:${record.type}:${labelSignature}@${positionSignature}${nestedSignature ? `[${nestedSignature}]` : ''}`;
    }

    return Object.keys(record)
        .sort()
        .map((key) => `${key}=${collectClusterElementsSignature(record[key])}`)
        .join('|');
}
