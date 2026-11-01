import {useEffect, useState} from 'react';

/*
 * True once isLoading has been false at least once. A later reload, such as a filter or environment change, does not
 * reset it, so a page can hide its left sidebar until it knows whether there is anything to filter without hiding it
 * again on every refetch.
 */
export const useInitialLoadComplete = (isLoading: boolean): boolean => {
    const [initialLoadComplete, setInitialLoadComplete] = useState(!isLoading);

    useEffect(() => {
        if (!isLoading) {
            setInitialLoadComplete(true);
        }
    }, [isLoading]);

    return initialLoadComplete || !isLoading;
};
