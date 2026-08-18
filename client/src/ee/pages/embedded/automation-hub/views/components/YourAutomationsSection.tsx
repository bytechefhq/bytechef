import {Table, TableBody, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import {SetAutomationEnabledRequestI} from '@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations';
import AutomationRow from '@/ee/pages/embedded/automation-hub/views/components/AutomationRow';
import {ConnectedUserProjectWorkflow} from '@/ee/shared/middleware/embedded/public';

interface YourAutomationsSectionProps {
    automations: ConnectedUserProjectWorkflow[];
    onDeleteAutomation: (workflowUuid: string) => void;
    onDeprovisionReference: (workflowUuid: string) => void;
    onSetEnabled: (request: SetAutomationEnabledRequestI) => void;
}

/**
 * Everything the connected user has that no published template accounts for: blank workflows they
 * created from scratch, copies whose source template has since been withdrawn, and dangling
 * references. Renders nothing at all when there is none of that — an empty heading is noise.
 */
const YourAutomationsSection = ({
    automations,
    onDeleteAutomation,
    onDeprovisionReference,
    onSetEnabled,
}: YourAutomationsSectionProps) => {
    if (!automations.length) {
        return null;
    }

    return (
        <section aria-labelledby="your-automations-heading">
            <h2 className="text-lg font-semibold" id="your-automations-heading">
                Your automations
            </h2>

            <Table className="mt-4">
                <TableHeader>
                    <TableRow>
                        <TableHead>Name</TableHead>

                        <TableHead>Apps</TableHead>

                        <TableHead>Status</TableHead>

                        <TableHead>Enabled</TableHead>

                        <TableHead />
                    </TableRow>
                </TableHeader>

                <TableBody>
                    {automations.map((automation) => (
                        <AutomationRow
                            automation={automation}
                            key={automation.workflowUuid}
                            onDeleteAutomation={onDeleteAutomation}
                            onDeprovisionReference={onDeprovisionReference}
                            onSetEnabled={onSetEnabled}
                        />
                    ))}
                </TableBody>
            </Table>
        </section>
    );
};

export default YourAutomationsSection;
