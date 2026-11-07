import {SchemaRecordType} from '@/components/JsonSchemaBuilder/utils/types';
import {getCookie} from '@/shared/util/cookie-utils';

export const generateSchemaFromSample = async (sampleJson: string): Promise<SchemaRecordType> => {
    const response = await fetch('/api/platform/internal/generate-schema', {
        body: sampleJson,
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        method: 'POST',
    });

    if (!response.ok) {
        throw new Error(`Schema generation failed with status ${response.status}`);
    }

    return await response.json();
};
