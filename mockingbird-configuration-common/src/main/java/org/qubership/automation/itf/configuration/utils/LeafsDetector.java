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
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Runnables;

/**
 * Adds leafs detector functionality to {@link AllRefsIterator}.
 * <br>Designed to be used in aggregation:
 * <br>{@link AllRefsIterator#getChildren(Object)} becomes {@link LeafsDetector#childrenSup}
 * <br>and {@link AllRefsIterator#backToParents()} becomes {@link LeafsDetector#backToParentsCb}.
 */
public class LeafsDetector<T> extends AllRefsIterator<T> {
    private final LinkedList<T> path = new LinkedList<>();
    private final Consumer<List<T>> leafsConsumer;
    private final Function<T, Iterator<? extends T>> childrenSup;
    private final Runnable backToParentsCb;

    /**
     * <br>Iterates over a tree hierarchy.
     * <br>The {@link Iterator#next()} invocation returns item of current iteration, same as {@link AllRefsIterator}.
     * <br>Reports about vertical level changes using childrenSup and backToParentsCb.
     * <br>Does not resolve recursion by default. You can do that in childrenSup or itemsFilter.
     * <br>Reports about leafs found using leafsConsumer.
     * <br>One iteration may result in invocation of leafsConsumer no more then one time.
     *
     * @param parents         to iterate over. Inclusive.
     * @param childrenSup     explains how to get children from parent. May return null.
     *                        Invoked when iteration goes deeper.
     * @param leafsConsumer   consumes path to leaf inclusive.
     *                        Leafs are detected based on result provided by childrenSup.
     * @param backToParentsCb Invoked when iteration goes upper to parents in a tree hierarchy.
     * @param itemsFilter     filters objects to iterate over. Delegates to {@link AllRefsIterator} itemsFilter.
     */
    public LeafsDetector(@Nonnull Iterator<? extends T> parents,
                         @Nonnull Function<T, Iterator<? extends T>> childrenSup,
                         @Nonnull Consumer<List<T>> leafsConsumer,
                         @Nullable Runnable backToParentsCb,
                         @Nullable Predicate<T> itemsFilter) {
        super(parents, itemsFilter == null ? always -> true : itemsFilter);
        this.leafsConsumer = leafsConsumer;
        this.childrenSup = childrenSup;
        this.backToParentsCb = backToParentsCb == null ? Runnables.doNothing() : backToParentsCb;
    }

    @Nullable
    @Override
    protected Iterator<? extends T> getChildren(@Nonnull T parent) {
        Iterator<? extends T> result = childrenSup.apply(parent);
        path.add(parent);
        if (result == null || !result.hasNext()) {
            leafsConsumer.accept(Lists.newArrayList(path));
        }
        return result;
    }

    @Override
    protected void backToParents() {
        if (!path.isEmpty()) {
            path.removeLast();
        }
        backToParentsCb.run();
    }
}
