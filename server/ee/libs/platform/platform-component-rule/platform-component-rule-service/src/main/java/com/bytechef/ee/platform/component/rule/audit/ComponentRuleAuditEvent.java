/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.rule.audit;

/**
 * @version ee
 * @author Ivica Cardic
 */
public enum ComponentRuleAuditEvent {

    RULE_BLOCKED(false),

    RULE_TAGGED(false),

    RULE_APPROVAL_REQUESTED(false),

    RULE_APPROVED(false),

    RULE_REJECTED(false),

    RULE_OBSERVED(false);

    private final boolean strictAudit;

    ComponentRuleAuditEvent(boolean strictAudit) {
        this.strictAudit = strictAudit;
    }

    public boolean isStrictAudit() {
        return strictAudit;
    }
}
