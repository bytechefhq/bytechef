import {type ProjectDeploymentsSheetStateI} from '@/pages/automation/project/components/ProjectDeploymentsSheet';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface ProjectHeaderDialogsStateI {
    deployDialogOpen: boolean;
    deploymentsSheetState: ProjectDeploymentsSheetStateI | undefined;
    publishPopoverOpen: boolean;
    setDeployDialogOpen: (open: boolean) => void;
    setDeploymentsSheetState: (deploymentsSheetState: ProjectDeploymentsSheetStateI | undefined) => void;
    setPublishPopoverOpen: (open: boolean) => void;
}

const useProjectHeaderDialogsStore = create<ProjectHeaderDialogsStateI>()(
    devtools((set) => ({
        deployDialogOpen: false,
        deploymentsSheetState: undefined,
        publishPopoverOpen: false,
        setDeployDialogOpen: (deployDialogOpen) => set(() => ({deployDialogOpen})),
        setDeploymentsSheetState: (deploymentsSheetState) => set(() => ({deploymentsSheetState})),
        setPublishPopoverOpen: (publishPopoverOpen) => set(() => ({publishPopoverOpen})),
    }))
);

export default useProjectHeaderDialogsStore;
