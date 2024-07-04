package com.bytechef.embedded.user.util;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.tenant.TenantContext;

/**
 * @author Ivica Cardic
 */
public class KeyId {

    private final long signingKeyId;
    private final String tenantId;

    private KeyId(long signingKeyId, String tenantId) {
        this.signingKeyId = signingKeyId;
        this.tenantId = tenantId;
    }

    public static KeyId of(long signingKeyId) {
        return new KeyId(signingKeyId, TenantContext.getCurrentTenantId());
    }

    public static KeyId parse(String id) {
        id = EncodingUtils.decodeBase64ToString(id);

        String[] items = id.split(":");

        return new KeyId(Long.parseLong(items[0]), items[1]);
    }

    @Override
    public String toString() {
        return EncodingUtils.encodeBase64ToString(tenantId + signingKeyId);
    }
}
