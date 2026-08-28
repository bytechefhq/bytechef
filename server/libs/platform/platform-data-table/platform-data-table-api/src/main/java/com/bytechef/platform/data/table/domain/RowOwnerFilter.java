/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.data.table.domain;

import com.bytechef.platform.owner.Owner;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Which rows of a data table a caller may see. {@link #unrestricted()} is every row, and is correct for admin and
 * automation callers; {@link #ownedBy(Owner)} narrows to that owner's rows plus the unowned, vendor-shared ones.
 *
 * <p>
 * The unrestricted case is a named factory rather than a null argument so that bypasses are greppable.
 *
 * @author Ivica Cardic
 */
public final class RowOwnerFilter {

    private static final RowOwnerFilter UNRESTRICTED = new RowOwnerFilter(null);

    private final @Nullable Owner owner;

    private RowOwnerFilter(@Nullable Owner owner) {
        this.owner = owner;
    }

    public static RowOwnerFilter unrestricted() {
        return UNRESTRICTED;
    }

    public static RowOwnerFilter ownedBy(Owner owner) {
        return new RowOwnerFilter(owner);
    }

    public static RowOwnerFilter from(Optional<Owner> owner) {
        return owner.map(RowOwnerFilter::ownedBy)
            .orElse(UNRESTRICTED);
    }

    public boolean isUnrestricted() {
        return owner == null;
    }

    public Optional<Owner> owner() {
        return Optional.ofNullable(owner);
    }
}
