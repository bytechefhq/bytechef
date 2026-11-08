import {useMcpServerCatalogQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

import {ConnectionPrefillOptionI} from '../types';
import {getMcpServerCatalogPrefillOption} from './mcpServerCatalogPrefill';

const STALE_TIME = 5 * 60 * 1000;

export default function useMcpServerCatalogPrefillOptions(enabled: boolean): ConnectionPrefillOptionI[] {
    const {data} = useMcpServerCatalogQuery(undefined, {enabled, retry: false, staleTime: STALE_TIME});

    return useMemo(
        () => (enabled && data?.mcpServerCatalog ? data.mcpServerCatalog.map(getMcpServerCatalogPrefillOption) : []),
        [data, enabled]
    );
}
