/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.automation.itf.configuration.utils;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.AbstractIterator;

/**
 * Wraps {@link LeafsDetector} to an iterator.
 * Returns leafs in a form of list consists of path to leaf and leaf itself.
 */
public abstract class LeafsIterator<T> extends AbstractIterator<List<T>> {
    private final LeafsDetector<T> leafsDetector;
    private List<T> leaf;

    public LeafsIterator(@Nonnull Iterator<? extends T> parents) {
        this.leafsDetector = new LeafsDetector<>(parents,
                this::getChildren,
                this::accept,
                null,
                null);
    }

    @Nullable
    protected abstract Iterator<? extends T> getChildren(@Nonnull T parent);

    private void accept(List<T> leaf) {
        this.leaf = leaf;
    }

    @Override
    protected List<T> computeNext() {
        while (leaf == null && leafsDetector.hasNext()) {
            leafsDetector.next();
        }
        if (leaf != null) {
            List<T> result = leaf;
            leaf = null;
            return result;
        }
        return endOfData();
    }
}
