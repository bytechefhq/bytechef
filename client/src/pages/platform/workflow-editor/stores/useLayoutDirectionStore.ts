import {DEFAULT_LAYOUT_DIRECTION, LayoutDirectionType} from '@/shared/constants';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface LayoutDirectionStateI {
    currentWorkflowId: string;
    directionsByWorkflow: Record<string, LayoutDirectionType>;
    layoutDirection: LayoutDirectionType;
    setLayoutDirection: (layoutDirection: LayoutDirectionType) => void;
    setWorkflowId: (workflowId: string) => void;
}

const useLayoutDirectionStore = create<LayoutDirectionStateI>()(
    devtools(
        persist(
            (set, get) => ({
                currentWorkflowId: '',
                directionsByWorkflow: {},
                layoutDirection: DEFAULT_LAYOUT_DIRECTION,

                setLayoutDirection: (layoutDirection) =>
                    set((state) => ({
                        directionsByWorkflow: {
                            ...state.directionsByWorkflow,
                            [state.currentWorkflowId]: layoutDirection,
                        },
                        layoutDirection,
                    })),

                setWorkflowId: (workflowId) => {
                    if (workflowId === get().currentWorkflowId) {
                        return;
                    }

                    set((state) => ({
                        currentWorkflowId: workflowId,
                        layoutDirection: state.directionsByWorkflow[workflowId] ?? DEFAULT_LAYOUT_DIRECTION,
                    }));
                },
            }),
            {
                migrate: (persisted, version) => {
                    if (version === 0) {
                        return {directionsByWorkflow: {}};
                    }

                    return persisted as {directionsByWorkflow: Record<string, LayoutDirectionType>};
                },
                name: 'bytechef.layout-direction',
                partialize: (state) => ({directionsByWorkflow: state.directionsByWorkflow}),
                version: 1,
            }
        )
    )
);

export default useLayoutDirectionStore;
