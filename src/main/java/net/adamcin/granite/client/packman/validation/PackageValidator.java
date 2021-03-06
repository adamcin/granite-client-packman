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
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.WspFilter;
import net.adamcin.granite.client.packman.WspFilter.Root;
import net.adamcin.granite.client.packman.WspFilter.Rule;
import net.adamcin.granite.client.packman.validation.ValidationResult.Reason;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.jackrabbit.vault.packaging.VaultPackage;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by madamcin on 3/14/14.
 */
public final class PackageValidator {
    private PackageValidator() {
    }

    /**
     * Validates a package file against a workspace filter. Validation consists of the following:
     *   1. Strict identify
     *   2. Scan file for forbidden extensions
     *   3. Call {@link org.apache.jackrabbit.vault.packaging.PackageManager#open(java.io.File, boolean)}
     *   4. Call {@link org.apache.jackrabbit.vault.packaging.VaultPackage#isValid()}
     *   5. Check package {@link org.apache.jackrabbit.vault.fs.api.WorkspaceFilter}
     *      against validation {@link org.apache.jackrabbit.vault.fs.api.WorkspaceFilter}
     *
     * @param file the package file to be validated
     * @param options the validation options
     * @return the validation result
     */
    public static ValidationResult validate(File file, ValidationOptions options) {
        if (file == null) {
            throw new NullPointerException("file");
        }

        try {
            PackId packageId = PackId.identifyPackage(file, true);
            if (packageId == null) {
                return new ValidationResult(Reason.FAILED_TO_ID);
            }
        } catch (IOException e) {
            return new ValidationResult(Reason.FAILED_TO_ID, e);
        }

        if (options.getForbiddenExtensions() != null) {
            try {
                JarFile jarFile = new JarFile(file);
                ValidationResult result =
                        checkForbiddenExtensions(jarFile,
                                options.getForbiddenExtensions());

                if (result.getReason() != Reason.SUCCESS) {
                    return result;
                }
            } catch (IOException e) {
                return ValidationResult.failedToOpen(e);
            }
        }

        return PackageValidator.validatePackage(file, options);
    }

    protected static ValidationResult validatePackage(File file, ValidationOptions options) {
        PackageManager manager = PackagingService.getPackageManager();
        VaultPackage pack = null;
        try {
            pack = manager.open(file, true);

            if (!pack.isValid()) {
                return new ValidationResult(Reason.INVALID_META_INF);
            }

            ValidationResult acHandlingResult = checkACHandling(options, pack);
            if (acHandlingResult.getReason() != Reason.SUCCESS) {
                return acHandlingResult;
            }

            ValidationResult forbiddenFilterRootPrefixResult = checkForbiddenFilterRootPrefix(options, pack);
            if (forbiddenFilterRootPrefixResult.getReason() != Reason.SUCCESS) {
                return forbiddenFilterRootPrefixResult;
            }

            ValidationResult deniedPathInclusionResult = checkDeniedPathInclusion(options, pack);
            if (deniedPathInclusionResult.getReason() != Reason.SUCCESS) {
                return deniedPathInclusionResult;
            }

            WspFilter archiveFilter =
                    WspFilter.adaptWorkspaceFilter(
                            pack.getMetaInf().getFilter());

            return checkFilter(options, archiveFilter);
        } catch (IOException e) {
            return new ValidationResult(Reason.FAILED_TO_OPEN, e);
        } finally {
            if (pack != null) {
                pack.close();
            }
        }
    }

    private static ACHandling modeForJKMode(AccessControlHandling jkMode) {
        if (jkMode == null) {
            return null;
        } else {
            switch (jkMode) {
                case IGNORE: return ACHandling.IGNORE;
                case OVERWRITE: return ACHandling.OVERWRITE;
                case MERGE: return ACHandling.MERGE;
                case MERGE_PRESERVE: return ACHandling.MERGE_PRESERVE;
                case CLEAR: return ACHandling.CLEAR;
                default: return null;
            }
        }
    }

    protected static ValidationResult checkACHandling(ValidationOptions options, VaultPackage pack) {
        List<ACHandling> forbidden = options.getForbiddenACHandlingModes();
        ACHandling jkMode = modeForJKMode(pack.getACHandling());
        if (forbidden != null && jkMode != null) {
            if (forbidden.contains(jkMode)) {
                return ValidationResult.forbiddenACHandlingMode(jkMode);
            }
        }
        return ValidationResult.success();
    }


    protected static ValidationResult checkForbiddenFilterRootPrefix(ValidationOptions options, VaultPackage pack) {
        List<String> forbiddenFilterRootPrefixes = options.getForbiddenFilterRootPrefixes();
        if (forbiddenFilterRootPrefixes != null) {
            for (String rootPrefix : forbiddenFilterRootPrefixes) {
                String trimmed = rootPrefix.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                final String noTrailingSlash = trimmed.replaceAll("/*$", "").replaceAll("^/*", "/");
                final String withTrailingSlash = noTrailingSlash.replaceAll("/*$", "/");
                for (PathFilterSet filterSet : pack.getMetaInf().getFilter().getFilterSets()) {
                    if (filterSet.getRoot().equals(noTrailingSlash)
                            || filterSet.getRoot().startsWith(withTrailingSlash)) {
                        WspFilter.Root invalidRoot = WspFilter.adaptFilterSet(filterSet);
                        return ValidationResult.forbiddenRootPrefix(rootPrefix, invalidRoot);
                    }
                }
            }
        }

        return ValidationResult.success();
    }

    protected static ValidationResult checkDeniedPathInclusion(ValidationOptions options, VaultPackage pack) {
        List<String> pathsDeniedForInclusion = options.getPathsDeniedForInclusion();
        if (pathsDeniedForInclusion != null) {
            for (String path : pathsDeniedForInclusion) {
                if (pack.getMetaInf().getFilter().contains(path)) {
                    WspFilter.Root invalidRoot = null;
                    PathFilterSet filter = pack.getMetaInf().getFilter().getCoveringFilterSet(path);
                    if (filter != null) {
                        invalidRoot = WspFilter.adaptFilterSet(filter);
                    }
                    return ValidationResult.deniedPathInclusion(path, invalidRoot);
                }
            }
        }
        return ValidationResult.success();
    }

    protected static ValidationResult checkFilter(ValidationOptions options, WspFilter archiveFilter) {
        WspFilter wspFilter = options.getValidationFilter();

        // skip filter check if validation filter is not specified
        if (wspFilter != null)  {
            WorkspaceFilter filter = convertToWorkspaceFilter(wspFilter);
            for (Root archiveRoot : archiveFilter.getRoots()) {
                String root = archiveRoot.getPath();

                if (filter.covers(root)) {
                    PathFilterSet covering = filter.getCoveringFilterSet(root);
                    Root coveringRoot =
                            WspFilter.adaptFilterSet(covering);

                    if (!hasRequiredRules(coveringRoot, archiveRoot)) {
                        return ValidationResult.rootMissingRules(archiveRoot, coveringRoot);
                    }
                } else if (!options.isAllowNonCoveredRoots()) {
                    return ValidationResult.rootNotAllowed(archiveRoot);
                }
            }
        }

        return ValidationResult.success();
    }

    protected static ValidationResult checkForbiddenExtensions(JarFile jarFile, List<String> forbiddenExtensions) throws IOException {
        if (forbiddenExtensions != null) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory()
                        && entry.getName().startsWith("jcr_root/")) {
                    String entryName = entry.getName();
                    for (String _ext : forbiddenExtensions) {
                        String ext = _ext.trim();
                        if (ext.isEmpty()) {
                            continue;
                        }

                        if (!ext.startsWith(".")) {
                            ext = "." + ext;
                        }

                        if (entryName.endsWith(ext)) {
                            return ValidationResult.forbiddenExtension(entryName);
                        }
                    }
                }
            }
        }
        return ValidationResult.success();
    }

    protected static boolean hasRequiredRules(Root coveringRoot, Root archiveRoot) {
        if (coveringRoot.getRules().size() > 0) {

            if (archiveRoot.getRules().size() < coveringRoot.getRules().size()) {

                return false;
            } else {
                for (int i = 0; i < coveringRoot.getRules().size(); i++) {
                    Rule validRule = coveringRoot.getRules().get((coveringRoot.getRules().size() - 1) - i);
                    Rule archiveRule =
                            archiveRoot.getRules().get((archiveRoot.getRules().size() - 1) - i);

                    if (validRule.isInclude() != archiveRule.isInclude() ||
                            !(validRule.getPattern().equals(archiveRule.getPattern()))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static WorkspaceFilter convertToWorkspaceFilter(WspFilter wspFilter) {
        DefaultWorkspaceFilter filter = new DefaultWorkspaceFilter();
        for (WspFilter.Root root : wspFilter.getRoots()) {
            PathFilterSet filterSet = new PathFilterSet(root.getPath());
            filter.add(filterSet);

            for (WspFilter.Rule rule : root.getRules()) {
                if (rule.isInclude()) {
                    filterSet.addInclude(new DefaultPathFilter(rule.getPattern()));
                } else {
                    filterSet.addExclude(new DefaultPathFilter(rule.getPattern()));
                }
            }
        }

        return filter;
    }
}
