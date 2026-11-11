import useProjectHeaderDialogsStore from '@/pages/automation/project/stores/useProjectHeaderDialogsStore';
import {beforeEach, describe, expect, it} from 'vitest';

describe('useProjectHeaderDialogsStore', () => {
    beforeEach(() => {
        useProjectHeaderDialogsStore.setState({deployDialogOpen: false, publishPopoverOpen: false});
    });

    it('starts with both dialogs closed', () => {
        const {deployDialogOpen, publishPopoverOpen} = useProjectHeaderDialogsStore.getState();

        expect(deployDialogOpen).toBe(false);
        expect(publishPopoverOpen).toBe(false);
    });

    it('opens and closes the publish popover independently of the deploy dialog', () => {
        useProjectHeaderDialogsStore.getState().setPublishPopoverOpen(true);

        expect(useProjectHeaderDialogsStore.getState().publishPopoverOpen).toBe(true);
        expect(useProjectHeaderDialogsStore.getState().deployDialogOpen).toBe(false);

        useProjectHeaderDialogsStore.getState().setPublishPopoverOpen(false);

        expect(useProjectHeaderDialogsStore.getState().publishPopoverOpen).toBe(false);
    });

    it('opens and closes the deploy dialog independently of the publish popover', () => {
        useProjectHeaderDialogsStore.getState().setDeployDialogOpen(true);

        expect(useProjectHeaderDialogsStore.getState().deployDialogOpen).toBe(true);
        expect(useProjectHeaderDialogsStore.getState().publishPopoverOpen).toBe(false);

        useProjectHeaderDialogsStore.getState().setDeployDialogOpen(false);

        expect(useProjectHeaderDialogsStore.getState().deployDialogOpen).toBe(false);
    });
});
