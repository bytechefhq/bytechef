/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import org.junit.jupiter.api.Test;

/**
 * {@code RV_RETURN_VALUE_IGNORED_INFERRED} is suppressed class-wide because the null-argument tests intentionally
 * invoke {@code divide(...)} / {@code multiply(...)} for their side effect of throwing {@link NullPointerException};
 * the return value is irrelevant to what the test is verifying.
 *
 * @version ee
 */
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_INFERRED")
class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency JPY = Currency.getInstance("JPY");

    @Test
    void testConstructorRejectsNullAmount() {
        assertThrows(NullPointerException.class, () -> new Money(null, USD));
    }

    @Test
    void testConstructorRejectsNullCurrency() {
        assertThrows(NullPointerException.class, () -> new Money(BigDecimal.ONE, null));
    }

    @Test
    void testConstructorNormalizesScaleForUsd() {
        // USD has 2 default fraction digits — sub-cent precision must not silently pass through into spend tracking.
        Money money = Money.usd(new BigDecimal("1.123456"));

        assertEquals(new BigDecimal("1.12"), money.amount());
        assertEquals(2, money.amount()
            .scale());
    }

    @Test
    void testConstructorNormalizesScaleForJpy() {
        // JPY has 0 default fraction digits — Money.of(1.99 JPY) rounds to 2.
        Money money = Money.of(new BigDecimal("1.99"), JPY);

        assertEquals(BigDecimal.valueOf(2), money.amount());
    }

    @Test
    void testConstructorUsesHalfEvenRounding() {
        // HALF_EVEN rounds 0.005 to 0.00 (nearest even), 0.015 to 0.02 (nearest even) — defuses cumulative bias
        // that plain HALF_UP would introduce in long-running spend aggregations.
        assertEquals(new BigDecimal("0.00"), Money.usd(new BigDecimal("0.005"))
            .amount());
        assertEquals(new BigDecimal("0.02"), Money.usd(new BigDecimal("0.015"))
            .amount());
    }

    @Test
    void testAddRejectsMixedCurrency() {
        Money usd = Money.usd(BigDecimal.ONE);
        Money eur = Money.of(BigDecimal.ONE, EUR);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> usd.add(eur));

        assertTrue(exception.getMessage()
            .contains("Currency mismatch"));
    }

    @Test
    void testSubtractRejectsMixedCurrency() {
        Money usd = Money.usd(BigDecimal.ONE);
        Money eur = Money.of(BigDecimal.ONE, EUR);

        assertThrows(IllegalArgumentException.class, () -> usd.subtract(eur));
    }

    @Test
    void testCompareToRejectsMixedCurrency() {
        Money usd = Money.usd(BigDecimal.ONE);
        Money eur = Money.of(BigDecimal.ONE, EUR);

        assertThrows(IllegalArgumentException.class, () -> usd.compareTo(eur));
    }

    @Test
    void testAddSumsAmountsWithSameCurrency() {
        Money result = Money.usd(new BigDecimal("1.25"))
            .add(Money.usd(new BigDecimal("0.75")));

        assertEquals(Money.usd(new BigDecimal("2.00")), result);
    }

    @Test
    void testSubtractProducesNegativeForOverdraw() {
        Money result = Money.usd(new BigDecimal("1.00"))
            .subtract(Money.usd(new BigDecimal("2.50")));

        assertTrue(result.isNegative());
        assertEquals(new BigDecimal("-1.50"), result.amount());
    }

    @Test
    void testMultiplyRescalesResult() {
        Money result = Money.usd(new BigDecimal("1.00"))
            .multiply(new BigDecimal("0.1234"));

        // 1.00 * 0.1234 = 0.123400 which rounds to 0.12 at USD scale
        assertEquals(new BigDecimal("0.12"), result.amount());
    }

    @Test
    void testMultiplyRejectsNullMultiplier() {
        assertThrows(NullPointerException.class, () -> Money.usd(BigDecimal.ONE)
            .multiply(null));
    }

    @Test
    void testDivideRescalesResult() {
        Money result = Money.usd(new BigDecimal("10.00"))
            .divide(new BigDecimal("3"), RoundingMode.HALF_EVEN);

        assertEquals(new BigDecimal("3.33"), result.amount());
    }

    @Test
    void testDivideRejectsNullArguments() {
        Money money = Money.usd(BigDecimal.ONE);

        assertThrows(NullPointerException.class, () -> money.divide(null, RoundingMode.HALF_EVEN));
        assertThrows(NullPointerException.class, () -> money.divide(BigDecimal.ONE, null));
    }

    @Test
    void testOfNonNegativeRejectsNegativeAmount() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Money.ofNonNegative(new BigDecimal("-0.01"), USD));

        assertTrue(exception.getMessage()
            .contains("must not be negative"));
    }

    @Test
    void testOfNonNegativeAcceptsZero() {
        Money zero = Money.ofNonNegative(BigDecimal.ZERO, USD);

        assertTrue(zero.isZero());
    }

    @Test
    void testIsZeroIsPositiveIsNegative() {
        assertTrue(Money.usd(BigDecimal.ZERO)
            .isZero());
        assertTrue(Money.usd(BigDecimal.ONE)
            .isPositive());
        assertFalse(Money.usd(BigDecimal.ONE)
            .isNegative());
        assertTrue(Money.usd(new BigDecimal("-1"))
            .isNegative());
    }

    @Test
    void testEqualsIsScaleNormalized() {
        // Scale-sensitive BigDecimal.equals would say 1.0 != 1.00 — that breaks Money as a map key and causes silent
        // aggregation bugs when values flow from different arithmetic paths.
        assertEquals(Money.usd(new BigDecimal("1.0")), Money.usd(new BigDecimal("1.00")));
        assertEquals(Money.usd(new BigDecimal("1.0"))
            .hashCode(),
            Money.usd(new BigDecimal("1.00"))
                .hashCode());
    }

    @Test
    void testEqualsRejectsDifferentCurrencyEvenWithSameAmount() {
        assertFalse(Money.usd(BigDecimal.ONE)
            .equals(Money.of(BigDecimal.ONE, EUR)));
    }

    @Test
    void testComparableOrderingWithinCurrency() {
        Money one = Money.usd(new BigDecimal("1.00"));
        Money two = Money.usd(new BigDecimal("2.00"));

        assertTrue(one.compareTo(two) < 0);
        assertTrue(two.compareTo(one) > 0);
        assertEquals(0, one.compareTo(Money.usd(new BigDecimal("1.00"))));
    }
}
