import {createContext, useContext} from 'react';

import {LAYOUT_DIRECTION, LayoutDirectionType} from './layout/constants';

// Live layout direction for the read-only node/edge components. WorkflowGraph provides the current
// (toggleable) value; nodes read it here instead of the static LAYOUT_DIRECTION default.
export const LayoutDirectionContext = createContext<LayoutDirectionType>(LAYOUT_DIRECTION);

export function useLayoutDirection(): LayoutDirectionType {
    return useContext(LayoutDirectionContext);
}
