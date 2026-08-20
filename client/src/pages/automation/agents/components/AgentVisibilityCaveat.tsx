import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {RadioIcon} from 'lucide-react';

/**
 * The one copy of the caveat every agent-visibility edit surface must carry.
 *
 * "Visibility" on an agent is easy to read as "who may talk to it", and it is not that: no runtime path in the
 * server consults visibility, so a PRIVATE agent keeps answering Slack, WhatsApp and its webhooks exactly as
 * before. It is one component rather than one copy per surface because a control whose name overpromises is
 * worse than no control, and a caveat that exists on the dialog but not on the list badge is the same thing.
 *
 * The remedy is named as well as the limitation — a caveat that only takes something away leaves the user
 * nowhere to go.
 */
const AgentVisibilityCaveat = () => (
    <Alert variant="warning">
        <RadioIcon />

        <AlertTitle>This does not stop the agent answering</AlertTitle>

        <AlertDescription>
            A private agent still replies to everyone on every channel it is deployed with — Slack, WhatsApp, its
            webhooks and in-app chat are unchanged. To stop it responding, remove its channels or undeploy it.
        </AlertDescription>
    </Alert>
);

export default AgentVisibilityCaveat;
