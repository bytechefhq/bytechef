import {AiGatewayRequestLog as AiGatewayRequestLogI} from '@/shared/middleware/graphql';
import {twMerge} from 'tailwind-merge';

interface AiGatewayRequestLogProps {
    requestLogs: Array<AiGatewayRequestLogI | null>;
}

const AiGatewayRequestLog = ({requestLogs}: AiGatewayRequestLogProps) => {
    const filteredLogs = requestLogs.filter(Boolean) as AiGatewayRequestLogI[];

    return (
        <div className="overflow-x-auto">
            {filteredLogs.length === 0 ? (
                <p className="py-4 text-center text-sm text-muted-foreground">No request logs found for this period.</p>
            ) : (
                <table className="w-full text-left text-sm">
                    <thead>
                        <tr className="border-b text-muted-foreground">
                            <th className="pb-2 font-medium">Timestamp</th>

                            <th className="pb-2 font-medium">Request ID</th>

                            <th className="pb-2 font-medium">Model</th>

                            <th className="pb-2 font-medium">Provider</th>

                            <th className="pb-2 font-medium">Latency</th>

                            <th className="pb-2 font-medium">Tokens (in/out)</th>

                            <th className="pb-2 font-medium">Cost</th>

                            <th className="pb-2 font-medium">Status</th>
                        </tr>
                    </thead>

                    <tbody>
                        {filteredLogs.map((log) => (
                            <tr className="border-b" key={log.id}>
                                <td className="py-2">
                                    {log.createdDate ? new Date(log.createdDate).toLocaleString() : '-'}
                                </td>

                                <td className="py-2 font-mono text-xs">{log.requestId}</td>

                                <td className="py-2">{log.routedModel || log.requestedModel || '-'}</td>

                                <td className="py-2">{log.routedProvider || '-'}</td>

                                <td className="py-2">{log.latencyMs != null ? `${log.latencyMs}ms` : '-'}</td>

                                <td className="py-2">
                                    {log.inputTokens ?? 0}/{log.outputTokens ?? 0}
                                </td>

                                <td className="py-2">
                                    {log.cost != null ? `$${parseFloat(log.cost).toFixed(4)}` : '-'}
                                </td>

                                <td className="py-2">
                                    <span
                                        className={twMerge(
                                            'rounded-full px-2 py-0.5 text-xs font-medium',
                                            log.status != null && log.status >= 200 && log.status < 300
                                                ? 'bg-green-100 text-green-800'
                                                : 'bg-red-100 text-red-800'
                                        )}
                                    >
                                        {log.status ?? 'N/A'}
                                    </span>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </div>
    );
};

export default AiGatewayRequestLog;
