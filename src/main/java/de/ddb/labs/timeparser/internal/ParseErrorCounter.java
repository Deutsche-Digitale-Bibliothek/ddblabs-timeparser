/* 
 * Copyright 2012-2026 Deutsche Digitale Bibliothek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ddb.labs.timeparser.internal;

import de.ddb.labs.timeparser.model.ParseErrorStats;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Mutable internal counter backing aggregated parse error statistics.
 */
@Getter
public final class ParseErrorCounter {
    private static final String UNKNOWN_VALUE = "-";
    private static final int MAX_STORED_VALUE_LENGTH = 256;

    @Getter(AccessLevel.NONE)
    private final AtomicInteger count = new AtomicInteger();
    private volatile String firstContext = UNKNOWN_VALUE;
    private volatile String firstInput = UNKNOWN_VALUE;
    private volatile String lastContext = UNKNOWN_VALUE;
    private volatile String lastInput = UNKNOWN_VALUE;
    private volatile String lastMessage = UNKNOWN_VALUE;

    public synchronized int incrementAndTrack(final String contextId, final String input, final String message) {
        final int currentCount = count.incrementAndGet();
        final String normalizedContext = normalizeLoggedValue(contextId);
        final String normalizedInput = normalizeLoggedValue(input);
        final String normalizedMessage = normalizeLoggedValue(message);

        if (currentCount == 1) {
            firstContext = normalizedContext;
            firstInput = normalizedInput;
        }

        lastContext = normalizedContext;
        lastInput = normalizedInput;
        lastMessage = normalizedMessage;
        return currentCount;
    }

    public synchronized ParseErrorStats snapshot() {
        return new ParseErrorStats(
                count.get(),
                firstContext,
                firstInput,
                lastContext,
                lastInput,
                lastMessage);
    }

    public int getCount() {
        return count.get();
    }

    private static String normalizeLoggedValue(final String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN_VALUE;
        }
        if (value.length() <= MAX_STORED_VALUE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_STORED_VALUE_LENGTH) + "...";
    }
}
