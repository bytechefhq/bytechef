package com.bytechef.ee.message.broker.aws.util;

import java.util.UUID;

public record AwsMessage(UUID uuid, String content) {
}
