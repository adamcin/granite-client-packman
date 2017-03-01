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

import net.adamcin.granite.client.packman.ACHandling;
import net.adamcin.granite.client.packman.WspFilter;

import java.io.IOException;
import java.io.Serializable;

/**
 * Encapsulation class for the various ways that a package validation can fail.
 */
public final class ValidationResult implements Serializable {

    private static final ValidationResult VALID = new ValidationResult(Reason.SUCCESS);
    private static final ValidationResult INVALID_META_INF = new ValidationResult(Reason.INVALID_META_INF);

    private static final long serialVersionUID = 3183860341927890671L;

    public enum Reason {
        SUCCESS,
        FORBIDDEN_EXTENSION,
        FAILED_TO_ID,
        FAILED_TO_OPEN,
        INVALID_META_INF,
        ROOT_NOT_ALLOWED,
        ROOT_MISSING_RULES,
        FORBIDDEN_ACHANDLING,
        FORBIDDEN_FILTER_ROOT_PREFIX,
        DENIED_PATH_INCLUSION
    }

    private final Reason reason;
    private final String forbiddenEntry;
    private final ACHandling forbiddenACHandlingMode;
    private final WspFilter.Root invalidRoot;
    private final WspFilter.Root coveringRoot;
    private final IOException cause;

    protected ValidationResult(Reason reason) {
        this(reason, (WspFilter.Root) null, null);
    }

    protected ValidationResult(Reason reason, WspFilter.Root invalidRoot, WspFilter.Root coveringRoot) {
        this.reason = reason;
        this.invalidRoot = invalidRoot;
        this.coveringRoot = coveringRoot;
        this.cause = null;
        this.forbiddenEntry = null;
        this.forbiddenACHandlingMode = null;
    }

    protected ValidationResult(Reason reason, IOException cause) {
        this.reason = reason;
        this.invalidRoot = null;
        this.coveringRoot = null;
        this.cause = cause;
        this.forbiddenEntry = null;
        this.forbiddenACHandlingMode = null;
    }

    protected ValidationResult(Reason reason, String forbiddenEntry) {
        this.reason = reason;
        this.invalidRoot = null;
        this.coveringRoot = null;
        this.cause = null;
        this.forbiddenEntry = forbiddenEntry;
        this.forbiddenACHandlingMode = null;
    }

    protected ValidationResult(Reason reason, String forbiddenEntry, WspFilter.Root invalidRoot) {
        this.reason = reason;
        this.invalidRoot = invalidRoot;
        this.coveringRoot = null;
        this.cause = null;
        this.forbiddenEntry = forbiddenEntry;
        this.forbiddenACHandlingMode = null;
    }

    protected ValidationResult(Reason reason, ACHandling forbiddenACHandlingMode) {
        this.reason = reason;
        this.invalidRoot = null;
        this.coveringRoot = null;
        this.cause = null;
        this.forbiddenEntry = null;
        this.forbiddenACHandlingMode = forbiddenACHandlingMode;
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

    public String getForbiddenEntry() {
        return forbiddenEntry;
    }

    public ACHandling getForbiddenACHandlingMode() {
        return forbiddenACHandlingMode;
    }

    public static ValidationResult success() {
        return VALID;
    }

    public static ValidationResult invalidMetaInf() {
        return INVALID_META_INF;
    }

    public static ValidationResult failedToId(IOException cause) {
        return new ValidationResult(Reason.FAILED_TO_ID, cause);
    }

    public static ValidationResult failedToOpen(IOException cause) {
        return new ValidationResult(Reason.FAILED_TO_OPEN, cause);
    }

    public static ValidationResult rootNotAllowed(WspFilter.Root invalidRoot) {
        return new ValidationResult(Reason.ROOT_NOT_ALLOWED, invalidRoot, null);
    }

    public static ValidationResult rootMissingRules(WspFilter.Root invalidRoot, WspFilter.Root coveringRoot) {
        return new ValidationResult(Reason.ROOT_MISSING_RULES, invalidRoot, coveringRoot);
    }

    public static ValidationResult forbiddenExtension(String forbiddenEntry) {
        return new ValidationResult(Reason.FORBIDDEN_EXTENSION, forbiddenEntry);
    }

    public static ValidationResult forbiddenACHandlingMode(ACHandling forbiddenMode) {
        return new ValidationResult(Reason.FORBIDDEN_ACHANDLING, forbiddenMode);
    }

    public static ValidationResult deniedPathInclusion(String forbiddenPath, WspFilter.Root invalidRoot) {
        return new ValidationResult(Reason.DENIED_PATH_INCLUSION, forbiddenPath, invalidRoot);
    }

    public static ValidationResult forbiddenRootPrefix(String forbiddenEntry, WspFilter.Root invalidRoot) {
        return new ValidationResult(Reason.FORBIDDEN_FILTER_ROOT_PREFIX, forbiddenEntry, invalidRoot);
    }
}
