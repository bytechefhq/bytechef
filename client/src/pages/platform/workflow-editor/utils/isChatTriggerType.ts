const CHAT_TRIGGER_TYPE_PATTERN = /^chat\/v\d+\/newChatRequest$/;

export default function isChatTriggerType(type: string | undefined) {
    return !!type && CHAT_TRIGGER_TYPE_PATTERN.test(type);
}
