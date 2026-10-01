import {type AiAgentDeploymentTrigger} from '@/shared/middleware/graphql';

const isScheduleTrigger = (trigger: Pick<AiAgentDeploymentTrigger, 'type'>): boolean =>
    trigger.type.split('/')[0] === 'schedule';

export default isScheduleTrigger;
