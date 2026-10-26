/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    private static final Currency USD = Currency.getInstance("USD");

    public Money {
        Validate.notNull(amount, "amount must not be null");
        Validate.notNull(currency, "currency must not be null");

        amount = normalizeScale(amount, currency);
    }

    private static BigDecimal normalizeScale(BigDecimal amount, Currency currency) {
        int fractionDigits = currency.getDefaultFractionDigits();

        if (fractionDigits < 0) {
            return amount;
        }

        return amount.setScale(fractionDigits, RoundingMode.HALF_EVEN);
    }

    public static Money usd(BigDecimal amount) {
        return new Money(amount, USD);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    public static Money ofNonNegative(BigDecimal amount, Currency currency) {
        Validate.notNull(amount, "amount must not be null");

        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                "amount must not be negative (was " + amount.toPlainString() + " " + currency.getCurrencyCode() + ")");
        }

        return new Money(amount, currency);
    }

    public static Money usdNonNegative(BigDecimal amount) {
        return ofNonNegative(amount, USD);
    }

    public Money add(Money other) {
        requireSameCurrency(other);

        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);

        return new Money(amount.subtract(other.amount), currency);
    }

    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "multiplier must not be null");

        return new Money(amount.multiply(multiplier), currency);
    }

    public Money divide(BigDecimal divisor, RoundingMode roundingMode) {
        Objects.requireNonNull(divisor, "divisor must not be null");
        Objects.requireNonNull(roundingMode, "roundingMode must not be null");

        int fractionDigits = currency.getDefaultFractionDigits();
        int scale = fractionDigits < 0 ? amount.scale() : fractionDigits;

        return new Money(amount.divide(divisor, scale, roundingMode), currency);
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);

        return amount.compareTo(other.amount);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Money money)) {
            return false;
        }

        return currency.equals(money.currency) && amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");

        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Currency mismatch: " + currency.getCurrencyCode() + " vs " + other.currency.getCurrencyCode());
        }
    }
}
