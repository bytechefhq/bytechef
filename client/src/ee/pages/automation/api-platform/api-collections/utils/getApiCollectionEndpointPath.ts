const trimSlashes = (segment?: string | null) => (segment ?? '').replace(/^\/+|\/+$/g, '');

export const getApiCollectionEndpointPath = (
    collectionVersion?: number | null,
    contextPath?: string | null,
    path?: string | null
) =>
    '/' +
    [`v${collectionVersion}`, trimSlashes(contextPath), trimSlashes(path)]
        .filter((segment) => segment !== '')
        .join('/');
