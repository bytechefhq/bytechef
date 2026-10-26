/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import com.bytechef.ee.platform.ai.llm.usage.Money;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public class BudgetExceededException extends RuntimeException {

    private final Money budget;
    private final Money spent;

    public BudgetExceededException(String message, Money budget, Money spent) {
        super(message);

        Objects.requireNonNull(budget, "budget must not be null");
        Objects.requireNonNull(spent, "spent must not be null");

        if (!budget.currency()
            .equals(spent.currency())) {
            throw new IllegalArgumentException(
                "budget/spent currency mismatch: " + budget.currency()
                    .getCurrencyCode() + " vs "
                    + spent.currency()
                        .getCurrencyCode());
        }

        this.budget = budget;
        this.spent = spent;
    }

    public Money getBudget() {
        return budget;
    }

    public Money getSpent() {
        return spent;
    }

    public BigDecimal getBudgetUsd() {
        return budget.amount();
    }

    public BigDecimal getSpentUsd() {
        return spent.amount();
    }
}
