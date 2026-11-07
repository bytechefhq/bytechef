import {createContext, useContext} from 'react';

import {LAYOUT_DIRECTION, LayoutDirectionType} from './layout/constants';

export const LayoutDirectionContext = createContext<LayoutDirectionType>(LAYOUT_DIRECTION);

export function useLayoutDirection(): LayoutDirectionType {
    return useContext(LayoutDirectionContext);
}
