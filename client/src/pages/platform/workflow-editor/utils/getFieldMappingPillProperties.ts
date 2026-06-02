import {PropertyType} from '@/shared/middleware/platform/configuration';

interface FieldMappingPillPropertyI {
    label: string;
    name: string;
    type: PropertyType;
}

/**
 * Builds synthetic child properties (one per applicationFields entry) from a field-mapping input's static test value,
 * so the data-pill panel renders a pill per application field. Accepts the mapObjectFields-shaped JSON (optionally
 * inside a `mapObjectFields` envelope); returns `[]` on invalid/empty input.
 */
export default function getFieldMappingPillProperties(testValue: string | undefined): Array<FieldMappingPillPropertyI> {
    if (!testValue) {
        return [];
    }

    try {
        const parsed = JSON.parse(testValue) as Record<
            string,
            {applicationFields?: {fields?: Array<{label: string; value: string}>}}
        >;

        const root = (parsed.mapObjectFields as typeof parsed) ?? parsed;
        const firstKey = Object.keys(root)[0];

        if (!firstKey) {
            return [];
        }

        const applicationFields = root[firstKey]?.applicationFields?.fields ?? [];

        return applicationFields.map((field) => ({label: field.label, name: field.value, type: PropertyType.String}));
    } catch {
        return [];
    }
}
