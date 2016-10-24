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

package net.adamcin.granite.client.packman;

import java.io.File;
import java.io.IOException;

/**
 * This is the Public API for a CRX Package Manager Console client. It is intended to
 * be used for implementation of higher level deployment management workflows, and
 * therefore it does not expose any mutable connection details.
 */
public interface PackageManagerClient {

    /**
     * Set the amount of time in milliseconds to wait for a response for any given
     * request. Set to a negative number for no timeout.
     * @param requestTimeout timeout in milliseconds
     */
    void setRequestTimeout(long requestTimeout);

    /**
     * Set the amount of time in milliseconds to wait for service availability,
     * which may span multiple retry requests. Set to a negative number for no timeout.
     * @param serviceTimeout timeout in milliseconds
     */
    void setServiceTimeout(long serviceTimeout);

    /**
     * Set a delay in milliseconds for calls to the {@link #waitForService()} method.
     * Increase this value to account for delayed OSGi bundle cycling on the server after
     * installing packages with embedded bundles.
     * Set this to a negative number or zero for no delay.
     * @param waitDelay delay in milliseconds
     */
    void setWaitDelay(long waitDelay);

    /**
     * @return the base URL (not including /crx.*) of the target server
     */
    String getBaseUrl();

    /**
     * Return the Console UI Url for the specified package, which is useful for creating HTML links.
     * @param packageId the {@link PackId} representing the package
     * @return the Console UI Url for the specified package, which is useful for creating HTML links.
     */
    String getConsoleUiUrl(PackId packageId);

    /**
     * Identify a CRX package based on its metadata. Will be strict about it so that it won't naively
     * pick up OSGi bundles if the client isn't careful about which files it is scanning.
     * @param file a {@link File} representing the package
     * @return a {@link PackId} object if the file represents a package, or {@code null} otherwise
     * @throws IOException if the file can not be read, or it is not a zip file
     */
    PackId identify(File file) throws IOException;
    
    /**
     * Identify a CRX package based on its metadata
     * @param file a {@link File} representing the package
     * @param strict set to true to require a META-INF/vault/properties.xml file.
     * @return a {@link PackId} object if the file represents a package, or {@code null} otherwise
     * @throws IOException if the file can not be read, or it is not a zip file
     */
    PackId identify(File file, boolean strict) throws IOException;

    /**
     * Wait for service availability. Use this method between installing a package and any calling
     * any other POST-based service operation
     * @return true if service availability was successfully determined, false otherwise
     * @throws Exception on timeout, interruption, or IOException
     */
    boolean waitForService() throws Exception;

    /**
     * Checks if a package with the specified packageId has already been uploaded to the server.
     * This does not indicate whether the package has already been installed.
     * @param packageId the {@link PackId} representing the package
     * @return {@code true} if a package exists, {@code false} otherwise
     * @throws Exception for unknown errors
     */
    boolean existsOnServer(PackId packageId) throws Exception;

    /**
     * List all packages
     * @return package list service response
     * @throws Exception for unknown errors
     */
    ListResponse list() throws Exception;

    /**
     * List all packages and filter by {@code query}
     * @param query can be null or empty string
     * @return package list service response filtered by {@code query}
     * @throws Exception for unknown errors
     */
    ListResponse list(String query) throws Exception;

    /**
     * List one package matching {@code packageId} or many packages matching the package group and name
     * up to the first hyphen.
     * @param packageId the {@link PackId} representing the group:name(:version)? to match
     * @param includeVersions set to true to match on group:name up to the first hyphen in the "name-version"
     *                        string, which effectively matches against all versions of the package
     * @return package list service response
     * @throws Exception for unknown errors
     */
    ListResponse list(PackId packageId, boolean includeVersions) throws Exception;

    /**
     * Upload a package to the server. Does not install the package once uploaded.
     * @param file the package file to be uploaded
     * @param force set to {@code true} for the uploaded file to replace an existing package on the
     *              server that has the same id. Has no effect if no existing package is found.
     * @param packageId optional {@link PackId} providing the installation path. If {@code null},
     *                  the {@code file} will be identified and that {@link PackId} will be used.
     * @return standard simple service response
     * @throws Exception for unknown errors
     */
    SimpleResponse upload(File file, boolean force, PackId packageId) throws Exception;

    /**
     * Downloads the package identified by {@code packageId} to the absolute path specified by {@code toFile}
     * @param packageId {@link PackId} representing package to be downloaded
     * @param toFile the file to save the downloaded binary data to.
     * @return a download service response
     * @throws Exception for unknown errors
     */
    DownloadResponse download(PackId packageId, File toFile) throws Exception;

    /**
     * Downloads the package to a qualified relative path under {@code toDirectory}
     * @param packageId {@link PackId} representing package to be downloaded
     * @param toDirectory a base directory under which packages will be saved at a relative path matching
     *                    their CRX installation path, starting with "./etc/packages"
     * @return a download service response
     * @throws Exception for unknown errors
     */
    DownloadResponse downloadToDirectory(PackId packageId, File toDirectory) throws Exception;

    /**
     * Delete a package from the server. Does not uninstall the package.
     * @param packageId {@link PackId} representing package to be deleted
     * @return standard simple service response
     * @throws Exception for unknown errors
     */
    SimpleResponse delete(PackId packageId) throws Exception;

    /**
     * Replicates the package using the server's default replication agents
     * @param packageId {@link PackId} representing package to be replicated
     * @return simple service response
     * @throws Exception for unknown errors
     */
    SimpleResponse replicate(PackId packageId) throws Exception;

    /**
     * Prints the contents of the package to the response
     * @param packageId {@link PackId} representing package
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse contents(PackId packageId) throws Exception;

    /**
     * Prints the contents of the package to the response
     * @param packageId {@link PackId} representing package
     * @param listener response progress listener
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse contents(PackId packageId, ResponseProgressListener listener) throws Exception;

    /**
     * Install a package that has already been uploaded to the server.
     * @param packageId {@link PackId} representing package to be installed
     * @param recursive set to {@code true} to also install subpackages
     * @param autosave number of changes between session saves.
     * @param acHandling Access Control Handling value {@link ACHandling}. Unspecified if {@code null}.
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse install(PackId packageId, boolean recursive, int autosave, ACHandling acHandling) throws Exception;

     /**
     * Install a package that has already been uploaded to the server.
     * @param packageId {@link PackId} representing package to be installed
     * @param recursive set to {@code true} to also install subpackages
     * @param autosave number of changes between session saves.
     * @param acHandling Access Control Handling value {@link ACHandling}. Unspecified if {@code null}.
     * @param listener response progress listener
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse install(PackId packageId, boolean recursive, int autosave,
                 ACHandling acHandling, ResponseProgressListener listener) throws Exception;

    /**
     * Performs a dryRun of an installation of the specified package
     * @param packageId {@link PackId} representing package
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse dryRun(PackId packageId) throws Exception;

    /**
     * Performs a dryRun of an installation of the specified package
     * @param packageId {@link PackId} representing package
     * @param listener response progress listener
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse dryRun(PackId packageId, ResponseProgressListener listener) throws Exception;

    /**
     * Create a package with the specified packageId
     * @param packageId {@link PackId} representing new package
     * @return simple service response
     * @throws Exception for unknown errors
     */
    SimpleResponse create(PackId packageId) throws Exception;

    /**
     * Update a package definition with the specified {@link WspFilter}
     * @param packageId {@link PackId} representing package to update
     * @param filter new workspace filter
     * @return simple service response
     * @throws Exception for unknown errors
     */
    SimpleResponse updateFilter(PackId packageId, WspFilter filter) throws Exception;

    /**
     * Move/Rename a package on the server
     * @param packageId {@link PackId} representing package to move/rename
     * @param moveToId {@link PackId} package ID to move to
     * @return simple service response
     * @throws Exception for unknown errors
     */
    SimpleResponse move(PackId packageId, PackId moveToId) throws Exception;

    /**
     * Builds the specified package
     * @param packageId {@link PackId} representing package
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse build(PackId packageId) throws Exception;

    /**
     * Builds the specified package
     * @param packageId {@link PackId} representing package
     * @param listener response progress listener
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse build(PackId packageId, ResponseProgressListener listener) throws Exception;

    /**
     * Rewraps the specified package
     * @param packageId {@link PackId} representing package
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse rewrap(PackId packageId) throws Exception;

    /**
     * Rewraps the specified package
     * @param packageId {@link PackId} representing package
     * @param listener response progress listener
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse rewrap(PackId packageId, ResponseProgressListener listener) throws Exception;

    /**
     * Uninstalls the specified package
     * @param packageId {@link PackId} representing package
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse uninstall(PackId packageId) throws Exception;

    /**
     * Uninstalls the specified package
     * @param packageId {@link PackId} representing package
     * @param listener response progress listener
     * @return detailed service response
     * @throws Exception for unknown errors
     */
    DetailedResponse uninstall(PackId packageId, ResponseProgressListener listener) throws Exception;

}
