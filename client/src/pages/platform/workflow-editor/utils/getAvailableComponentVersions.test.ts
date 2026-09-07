import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getAvailableComponentVersions from './getAvailableComponentVersions';

const componentDefinitionVersion = (version: number) => ({name: 'test', version}) as ComponentDefinitionBasic;

describe('getAvailableComponentVersions', () => {
    it('should list every registered version in ascending order', () => {
        expect(
            getAvailableComponentVersions({
                componentDefinitionVersions: [componentDefinitionVersion(2), componentDefinitionVersion(1)],
                nodeVersion: '2',
            })
        ).toEqual([1, 2]);
    });

    it('should include the version of the node when the versions have not been fetched yet', () => {
        expect(getAvailableComponentVersions({componentDefinitionVersions: undefined, nodeVersion: '2'})).toEqual([2]);
    });

    it('should include the version of the node when it is no longer registered', () => {
        expect(
            getAvailableComponentVersions({
                componentDefinitionVersions: [componentDefinitionVersion(3)],
                nodeVersion: '1',
            })
        ).toEqual([1, 3]);
    });

    it('should not repeat the version of the node', () => {
        expect(
            getAvailableComponentVersions({
                componentDefinitionVersions: [componentDefinitionVersion(1), componentDefinitionVersion(2)],
                nodeVersion: '1',
            })
        ).toEqual([1, 2]);
    });

    it('should ignore a node version that is not a number', () => {
        expect(
            getAvailableComponentVersions({
                componentDefinitionVersions: [componentDefinitionVersion(1)],
                nodeVersion: '',
            })
        ).toEqual([1]);
    });
});
