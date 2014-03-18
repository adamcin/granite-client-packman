/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.granite.client.packman.validation;

import net.adamcin.granite.client.packman.WspFilter;

import java.io.IOException;

/**
 * Encapsulation class for the various ways that a package validation can fail.
 */
public final class ValidationResult {

    public static final ValidationResult VALID = new ValidationResult(Reason.SUCCESS);

    public static enum Reason {
        SUCCESS,
        FAILED_TO_ID,
        FAILED_TO_OPEN,
        INVALID_META_INF,
        ROOT_NOT_ALLOWED,
        ROOT_MISSING_RULES
    }

    private final Reason reason;
    private final WspFilter.Root invalidRoot;
    private final WspFilter.Root coveringRoot;
    private final IOException cause;

    public ValidationResult(Reason reason) {
        this(reason, null, null);
    }

    public ValidationResult(Reason reason, WspFilter.Root invalidRoot) {
        this(reason, invalidRoot, null);
    }

    public ValidationResult(Reason reason, WspFilter.Root invalidRoot, WspFilter.Root coveringRoot) {
        this.reason = reason;
        this.invalidRoot = invalidRoot;
        this.coveringRoot = coveringRoot;
        this.cause = null;
    }

    public ValidationResult(Reason reason, IOException cause) {
        this.reason = reason;
        this.invalidRoot = null;
        this.coveringRoot = null;
        this.cause = cause;
    }

    public Reason getReason() {
        return reason;
    }

    public WspFilter.Root getInvalidRoot() {
        return invalidRoot;
    }

    public WspFilter.Root getCoveringRoot() {
        return coveringRoot;
    }

    public IOException getCause() {
        return cause;
    }
}
