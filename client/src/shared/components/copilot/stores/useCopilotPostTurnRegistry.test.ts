import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useCopilotPostTurnRegistry from './useCopilotPostTurnRegistry';

describe('useCopilotPostTurnRegistry', () => {
    beforeEach(() => {
        useCopilotPostTurnRegistry.setState({callbacks: {}});
    });

    it('should run every callback registered for a source', () => {
        const listCallback = vi.fn();
        const detailCallback = vi.fn();

        const {register} = useCopilotPostTurnRegistry.getState();

        register(Source.DATA_TABLE, listCallback);
        register(Source.DATA_TABLE, detailCallback);

        useCopilotPostTurnRegistry.getState().runFor(Source.DATA_TABLE);

        expect(listCallback).toHaveBeenCalledTimes(1);
        expect(detailCallback).toHaveBeenCalledTimes(1);
    });

    it('should unregister only the callback that owns the returned cleanup', () => {
        const listCallback = vi.fn();
        const detailCallback = vi.fn();

        const {register} = useCopilotPostTurnRegistry.getState();

        const unregisterList = register(Source.DATA_TABLE, listCallback);

        register(Source.DATA_TABLE, detailCallback);

        unregisterList();

        useCopilotPostTurnRegistry.getState().runFor(Source.DATA_TABLE);

        expect(listCallback).not.toHaveBeenCalled();
        expect(detailCallback).toHaveBeenCalledTimes(1);
    });

    it('should not run callbacks registered for another source', () => {
        const dataTableCallback = vi.fn();

        useCopilotPostTurnRegistry.getState().register(Source.DATA_TABLE, dataTableCallback);

        useCopilotPostTurnRegistry.getState().runFor(Source.KNOWLEDGE_BASE);

        expect(dataTableCallback).not.toHaveBeenCalled();
    });

    it('should tolerate a source with no registrations', () => {
        expect(() => useCopilotPostTurnRegistry.getState().runFor(Source.SKILLS)).not.toThrow();
    });

    it('should keep the first registration alive when the second unregisters', () => {
        const listCallback = vi.fn();
        const detailCallback = vi.fn();

        const {register} = useCopilotPostTurnRegistry.getState();

        register(Source.DATA_TABLE, listCallback);

        const unregisterDetail = register(Source.DATA_TABLE, detailCallback);

        unregisterDetail();

        useCopilotPostTurnRegistry.getState().runFor(Source.DATA_TABLE);

        expect(listCallback).toHaveBeenCalledTimes(1);
        expect(detailCallback).not.toHaveBeenCalled();
    });

    it('should remove only one registration when the same callback is registered twice', () => {
        const sharedCallback = vi.fn();

        const {register} = useCopilotPostTurnRegistry.getState();

        const unregisterFirst = register(Source.DATA_TABLE, sharedCallback);

        register(Source.DATA_TABLE, sharedCallback);

        unregisterFirst();
        unregisterFirst();

        useCopilotPostTurnRegistry.getState().runFor(Source.DATA_TABLE);

        expect(sharedCallback).toHaveBeenCalledTimes(1);
    });
});
