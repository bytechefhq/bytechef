import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {describe, expect, it} from 'vitest';

describe('applicationInfoStore', () => {
    describe('initial state', () => {
        it('exposes a fully-shaped ai object before getApplicationInfo resolves', () => {
            const {ai} = applicationInfoStore.getState();

            expect(ai.copilot.enabled).toBe(false);
            expect(ai.gateway.enabled).toBe(false);
            expect(ai.hub.enabled).toBe(false);
            expect(ai.knowledgeBase.enabled).toBe(false);
        });

        it('defaults the MCP server toggle to enabled before getApplicationInfo resolves', () => {
            // Mirrors the server-side default: bytechef.ai.mcp.server.enabled uses
            // matchIfMissing = true, so an absent property means the server runs and
            // the settings menu entry should show.
            const {ai} = applicationInfoStore.getState();

            expect(ai.mcp.server.enabled).toBe(true);
        });

        it('defaults custom component Java uploads to enabled before getApplicationInfo resolves', () => {
            const {component} = applicationInfoStore.getState();

            expect(component.customComponent.javaEnabled).toBe(true);
        });

        it('defaults code workflow Java uploads to enabled before getApplicationInfo resolves', () => {
            const {workflow} = applicationInfoStore.getState();

            expect(workflow.codeWorkflow.javaEnabled).toBe(true);
        });
    });
});
