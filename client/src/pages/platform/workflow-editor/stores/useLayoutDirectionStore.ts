import {DEFAULT_LAYOUT_DIRECTION, LayoutDirectionType} from '@/shared/constants';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface LayoutDirectionStateI {
    currentWorkflowUuid: string;
    directionsByWorkflowUuid: Record<string, LayoutDirectionType>;
    layoutDirection: LayoutDirectionType;
    setCurrentWorkflowUuid: (workflowUuid: string) => void;
    setLayoutDirection: (layoutDirection: LayoutDirectionType) => void;
}

const useLayoutDirectionStore = create<LayoutDirectionStateI>()(
    devtools(
        persist(
            (set, get) => ({
                currentWorkflowUuid: '',
                directionsByWorkflowUuid: {},
                layoutDirection: DEFAULT_LAYOUT_DIRECTION,

                setCurrentWorkflowUuid: (workflowUuid) => {
                    if (workflowUuid === get().currentWorkflowUuid) {
                        return;
                    }

                    set((state) => ({
                        currentWorkflowUuid: workflowUuid,
                        layoutDirection: state.directionsByWorkflowUuid[workflowUuid] ?? DEFAULT_LAYOUT_DIRECTION,
                    }));
                },

                setLayoutDirection: (layoutDirection) =>
                    set((state) => ({
                        directionsByWorkflowUuid: {
                            ...state.directionsByWorkflowUuid,
                            [state.currentWorkflowUuid]: layoutDirection,
                        },
                        layoutDirection,
                    })),
            }),
            {
                migrate: (persisted, version) => {
                    if (version < 2) {
                        return {directionsByWorkflowUuid: {}};
                    }

                    return persisted as {directionsByWorkflowUuid: Record<string, LayoutDirectionType>};
                },
                name: 'bytechef.layout-direction',
                partialize: (state) => ({directionsByWorkflowUuid: state.directionsByWorkflowUuid}),
                version: 2,
            }
        )
    )
);

export default useLayoutDirectionStore;
