import {useId, useMemo} from 'react';

import {jsonSchemaModelPath} from './jsonSchemaCompletion';

export function useJsonSchemaModelPath(): string {
    const editorId = useId();

    return useMemo(() => jsonSchemaModelPath(editorId), [editorId]);
}
