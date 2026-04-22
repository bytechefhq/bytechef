/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * Currency-aware money amount used by the AI Gateway for costs, budget limits, alert thresholds, and spend
 * aggregations. Prevents bugs from summing or comparing raw {@code BigDecimal} cost fields across currencies.
 *
 * <p>
 * <b>Invariants:</b>
 * <ul>
 * <li>{@code amount} and {@code currency} are non-null.</li>
 * <li>{@code amount} is rounded to the currency's default fraction digits (HALF_EVEN) at construction — prevents
 * sub-cent amounts like {@code Money.usd("1.123456")} from silently propagating through spend tracking.
 * Pseudo-currencies with {@code getDefaultFractionDigits() == -1} (e.g. XTS, XXX) skip scale normalization.</li>
 * <li>Arithmetic helpers ({@link #add}, {@link #subtract}, {@link #compareTo}) reject mixed-currency operands.</li>
 * <li>{@link #multiply} and {@link #divide} preserve currency and re-apply the currency-appropriate scale.</li>
 * <li>{@link #ofNonNegative} rejects negative amounts at construction — use it for inputs that should never be negative
 * (recorded costs, budget limits, alert thresholds).</li>
 * <li>{@link #equals}/{@link #hashCode} are scale-normalized: {@code Money.usd("1.0")} equals
 * {@code Money.usd("1.00")}. The compiler-generated record equals compared scales, which causes silent aggregation bugs
 * when values flow in from different arithmetic paths.</li>
 * <li>Implements {@link Comparable}, so generic sort/min/max utilities work and {@code TreeMap}/{@code TreeSet} accept
 * Money keys. Mixed-currency compare is rejected.</li>
 * </ul>
 *
 * @version ee
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

    /**
     * Strict factory for values that must not be negative (recorded costs, budget amounts, alert thresholds). Callers
     * that can legitimately produce negative values (e.g. {@code remaining = budget - spend}) should use {@link #of}
     * instead.
     */
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

    /**
     * Multiplies this money by a scalar. The result is rescaled to the currency's default fraction digits by the
     * compact constructor — callers don't have to worry about intermediate precision drift.
     */
    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "multiplier must not be null");

        return new Money(amount.multiply(multiplier), currency);
    }

    /**
     * Divides this money by a scalar using the given rounding mode. Rounding mode is required because BigDecimal
     * division of non-terminating decimals (e.g. 1/3) would otherwise throw {@link ArithmeticException}.
     */
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

    /**
     * Scale-normalized equality: {@code Money.usd("1.0")} equals {@code Money.usd("1.00")}. The record's default equals
     * uses {@link BigDecimal#equals} which is scale-sensitive — that breaks use as a map key and causes silent
     * aggregation bugs when values from different arithmetic paths are compared.
     */
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
