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

package org.qubership.automation.itf.core.startup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Error {

    private String id;

    private String place;

    private String message;

    private Throwable exception;

    public Error(@Nonnull String place, @Nonnull String message, @Nullable Throwable exception) {
        this.place = place;
        this.message = message;
        this.exception = exception;
    }

    public Error() {
    }

    public String getPlace() {
        return place;
    }

    /**
     * @param place where exception produced
     *              This param need for preview that exceptions occurred on starup.
     *              "Datasets reading" for example.
     */
    public void setPlace(@Nonnull String place) {
        this.place = place;
    }

    public String getMessage() {
        return message;
    }

    /**
     * @param message what happen
     *                "Duplication of datasets 'DSNAME'" for example
     */
    public void setMessage(@Nonnull String message) {
        this.message = message;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(@Nullable Throwable exception) {
        this.exception = exception;
    }

    public String getId() {
        return id;
    }
}
