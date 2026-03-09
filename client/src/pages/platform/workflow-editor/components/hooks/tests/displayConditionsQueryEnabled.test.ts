import {describe, expect, it} from 'vitest';

/**
 * Replicates the `enabled` condition for the display conditions queries
 * in useWorkflowNodeDetailsPanel.ts (lines 341-345 and 356-361).
 *
 * Tests that display conditions queries only fire when the Properties tab
 * is active, preventing unnecessary API calls on other tabs and avoiding
 * race conditions when a node is added optimistically but not yet persisted.
 */

interface DisplayConditionsEnabledParamsI {
    activeTab: string;
    currentClusterElementName?: string;
    currentNodeClusterElementType?: string;
    currentNodeName?: string;
}

function isDisplayConditionsQueryEnabled({
    activeTab,
    currentClusterElementName,
    currentNodeClusterElementType,
    currentNodeName,
}: DisplayConditionsEnabledParamsI): boolean {
    return (
        activeTab === 'properties' &&
        !!currentNodeName &&
        currentNodeName !== 'manual' &&
        currentNodeName !== currentClusterElementName &&
        !currentNodeClusterElementType
    );
}

function isClusterElementDisplayConditionsQueryEnabled({
    activeTab,
    currentClusterElementName,
    currentNodeClusterElementType,
    currentNodeName,
}: DisplayConditionsEnabledParamsI): boolean {
    return (
        activeTab === 'properties' &&
        !!currentNodeName &&
        currentNodeName !== 'manual' &&
        currentNodeName === currentClusterElementName &&
        !!currentNodeClusterElementType
    );
}

describe('Display Conditions Query Enabled', () => {
    describe('regular node query', () => {
        const baseParams: DisplayConditionsEnabledParamsI = {
            activeTab: 'properties',
            currentNodeName: 'activeCampaign_1',
        };

        it('should be enabled on properties tab with a valid node', () => {
            expect(isDisplayConditionsQueryEnabled(baseParams)).toBe(true);
        });

        it('should be disabled on description tab', () => {
            expect(isDisplayConditionsQueryEnabled({...baseParams, activeTab: 'description'})).toBe(false);
        });

        it('should be disabled on connection tab', () => {
            expect(isDisplayConditionsQueryEnabled({...baseParams, activeTab: 'connection'})).toBe(false);
        });

        it('should be disabled on output tab', () => {
            expect(isDisplayConditionsQueryEnabled({...baseParams, activeTab: 'output'})).toBe(false);
        });

        it('should be disabled when currentNodeName is undefined', () => {
            expect(isDisplayConditionsQueryEnabled({...baseParams, currentNodeName: undefined})).toBe(false);
        });

        it('should be disabled for manual trigger', () => {
            expect(isDisplayConditionsQueryEnabled({...baseParams, currentNodeName: 'manual'})).toBe(false);
        });

        it('should be disabled when node is a cluster element type', () => {
            expect(
                isDisplayConditionsQueryEnabled({
                    ...baseParams,
                    currentNodeClusterElementType: 'tools',
                })
            ).toBe(false);
        });

        it('should be disabled when node matches cluster element name', () => {
            expect(
                isDisplayConditionsQueryEnabled({
                    ...baseParams,
                    currentClusterElementName: 'activeCampaign_1',
                })
            ).toBe(false);
        });
    });

    describe('cluster element query', () => {
        const baseParams: DisplayConditionsEnabledParamsI = {
            activeTab: 'properties',
            currentClusterElementName: 'tools_1',
            currentNodeClusterElementType: 'tools',
            currentNodeName: 'tools_1',
        };

        it('should be enabled on properties tab for cluster element', () => {
            expect(isClusterElementDisplayConditionsQueryEnabled(baseParams)).toBe(true);
        });

        it('should be disabled on description tab', () => {
            expect(isClusterElementDisplayConditionsQueryEnabled({...baseParams, activeTab: 'description'})).toBe(
                false
            );
        });

        it('should be disabled on connection tab', () => {
            expect(isClusterElementDisplayConditionsQueryEnabled({...baseParams, activeTab: 'connection'})).toBe(false);
        });

        it('should be disabled when node name does not match cluster element name', () => {
            expect(
                isClusterElementDisplayConditionsQueryEnabled({
                    ...baseParams,
                    currentNodeName: 'other_1',
                })
            ).toBe(false);
        });
    });

    describe('optimistic panel opening race condition', () => {
        it('should not fire query when panel opens on description tab after adding a node', () => {
            // When a node is added optimistically, the panel opens on the description tab.
            // The display conditions query should NOT fire yet because the server
            // may not have persisted the node.
            expect(
                isDisplayConditionsQueryEnabled({
                    activeTab: 'description',
                    currentNodeName: 'activeCampaign_1',
                })
            ).toBe(false);
        });

        it('should fire query only when user switches to properties tab', () => {
            // By the time the user clicks Properties, the mutation has completed.
            expect(
                isDisplayConditionsQueryEnabled({
                    activeTab: 'properties',
                    currentNodeName: 'activeCampaign_1',
                })
            ).toBe(true);
        });
    });
});
