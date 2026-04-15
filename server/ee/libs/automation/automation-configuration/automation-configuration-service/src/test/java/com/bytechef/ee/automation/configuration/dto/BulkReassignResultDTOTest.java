/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.dto;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.bytechef.ee.automation.configuration.dto.BulkReassignResultDTO.BulkReassignFailureDTO;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class BulkReassignResultDTOTest {

    @Test
    void testAcceptsCountsThatAccountForEveryRow() {
        assertThatCode(
            () -> new BulkReassignResultDTO(
                3, 1, 1, 1, List.of(BulkReassignFailureDTO.of(7L, "UNEXPECTED", "Unexpected error"))))
                    .doesNotThrowAnyException();
    }

    @Test
    void testRejectsRowsLeftUnaccountedFor() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BulkReassignResultDTO(3, 1, 1, 0, List.of()));
    }
}
