import {describe, expect, it} from 'vitest';

import {type ExternalInvocationLogItemType, invocationNameLabel} from '../externalInvocationLabels';

const externalInvocationLog = (source: string, invocationName: string | null) =>
    ({invocationName, source}) as ExternalInvocationLogItemType;

describe('invocationNameLabel', () => {
    it.each([
        ['MCP_SERVER', 'slack_sendMessage', 'SLACK_SEND_MESSAGE'],
        ['MCP_SERVER', 'Echo_Message', 'ECHO_MESSAGE'],
        ['MCP_SERVER', 'MCP_Tool_Test_Greeting', 'MCP_TOOL_TEST_GREETING'],
        ['MCP_SERVER', 'SEND_EMAIL', 'SEND_EMAIL'],
        ['MCP_SERVER', 'order-sync2', 'ORDER-SYNC2'],
        ['API_TOOL', 'googlemail_send_email', 'GOOGLEMAIL_SEND_EMAIL'],
    ])('shows the %s tool name %s as %s', (source, invocationName, expected) => {
        expect(invocationNameLabel(externalInvocationLog(source, invocationName))).toBe(expected);
    });

    it.each([
        ['API_ACTION', 'sendMessage'],
        ['A2A_SERVER', 'Summarize'],
    ])('keeps the %s name %s as it was called', (source, invocationName) => {
        expect(invocationNameLabel(externalInvocationLog(source, invocationName))).toBe(invocationName);
    });

    it('returns null when nothing was recorded', () => {
        expect(invocationNameLabel(externalInvocationLog('MCP_SERVER', null))).toBeNull();
    });
});
