/* eslint-disable sort-keys */

import {ComponentDataType} from '@/types/types';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface ComponentDataState {
    componentData: Array<ComponentDataType>;
    setComponentData: (componentData: Array<ComponentDataType>) => void;
}

const useComponentDataStore = create<ComponentDataState>()(
    devtools(
        (set) => ({
            componentData: [],
            setComponentData: (componentData) => set(() => ({componentData})),
        }),
        {
            name: 'component-data',
        }
    )
);

export default useComponentDataStore;
