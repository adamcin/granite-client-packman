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
 * This is the Public API for a CRX Package Manager Console client. It is intended to be used for implementation of
 * higher level deployment management workflows, and therefore it does not expose any connection details.
 */
public interface PackageManagerClient {

    /**
     *
     * @param requestTimeout
     */
    void setRequestTimeout(long requestTimeout);

    /**
     *
     * @param serviceTimeout the amount of time to wait for service availability
     */
    void setServiceTimeout(long serviceTimeout);

    String getBaseUrl();

    String getLoginUrl();

    /**
     *
     * @param packageId
     * @return
     */
    String getConsoleUiUrl(PackId packageId);

    /**
     * Identify a CRX package based on its metadata
     * @param file a {@link File} representing the package
     * @return a {@link PackId} object if the file represents a package, or {@code null} otherwise
     * @throws IOException if the file can not be read, or it is not a zip file
     */
    PackId identify(File file) throws IOException;

    /**
     * Wait for service availability. Use this method between installing a package and any calling any other POST-based
     * service operation
     * @throws Exception on timeout, interruption, or IOException
     */
    void waitForService() throws Exception;

    /**
     * Checks if a package with the specified packageId has already been uploaded to the server. This does not indicate
     * whether the package has already been installed.
     * @param packageId
     * @return {@code true} if a package exists, {@code false} otherwise
     * @throws Exception
     */
    boolean existsOnServer(PackId packageId) throws Exception;

    /**
     * List all packages
     * @return list of packages
     * @throws Exception
     */
    ListResponse list() throws Exception;

    /**
     * List all packages and filter by {@code query}
     * @param query can be null or empty string
     * @return list of packages filtered by {@code query}
     * @throws Exception
     */
    ListResponse list(String query) throws Exception;

    /**
     * List one package matching {@code packageId} or many packages matching the {@code packageId} up to the first hyphen.
     * @param packageId
     * @param includeVersions
     * @return
     * @throws Exception
     */
    ListResponse list(PackId packageId, boolean includeVersions) throws Exception;

    /**
     * Upload a package to the server. Does not install the package once uploaded.
     * @param file the package file to be uploaded
     * @param force set to {@code true} for the uploaded file to replace an existing package on the server that has the
     *              same id. If {@code false}
     * @param packageId optional {@link PackId} providing the installation path. If {@code null}, the {@code file} will
     *                  be identified and that {@link PackId} will be used
     * @return standard simple service response
     * @throws Exception
     */
    SimpleResponse upload(File file, boolean force, PackId packageId) throws Exception;

    /**
     * Downloads the package identified by {@code packageId} to the absolute path specified by {@code toFile}
     * @param packageId
     * @param toFile
     * @return
     * @throws Exception
     */
    DownloadResponse download(PackId packageId, File toFile) throws Exception;

    /**
     * Downloads the package to a qualified relative path under {@code toDirectory}
     * @param packageId
     * @param toDirectory
     * @return
     * @throws Exception
     */
    DownloadResponse downloadToDirectory(PackId packageId, File toDirectory) throws Exception;

    /**
     * Delete a package from the server. Does not uninstall the package.
     * @param packageId {@link PackId} representing package to be deleted
     * @return standard simple service response
     * @throws Exception
     */
    SimpleResponse delete(PackId packageId) throws Exception;

    /**
     * Replicates the package using the server's default replication agents
     * @param packageId {@link PackId} representing package to be replicated
     * @return simple service response
     * @throws Exception
     */
    SimpleResponse replicate(PackId packageId) throws Exception;

    DetailedResponse contents(PackId packageId) throws Exception;

    DetailedResponse contents(PackId packageId, ResponseProgressListener listener) throws Exception;

    /**
     * Install a package that has already been uploaded to the server.
     * @param packageId {@link PackId} representing package to be installed
     * @param recursive set to {@code true} to also install subpackages
     * @param autosave number of changes between session saves.
     * @param acHandling Access Control Handling value {@link ACHandling}. Unspecified if {@code null}.
     * @return detailed service response
     * @throws Exception
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
     * @throws Exception
     */
    DetailedResponse install(PackId packageId, boolean recursive, int autosave,
                 ACHandling acHandling, ResponseProgressListener listener) throws Exception;

    /**
     * Performs a dryRun of an installation of the specified package
     * @param packageId
     * @return
     * @throws Exception
     */
    DetailedResponse dryRun(PackId packageId) throws Exception;

    /**
     * Performs a dryRun of an installation of the specified package
     * @param packageId
     * @param listener
     * @return
     * @throws Exception
     */
    DetailedResponse dryRun(PackId packageId, ResponseProgressListener listener) throws Exception;

    DetailedResponse build(PackId packageId) throws Exception;

    DetailedResponse build(PackId packageId, ResponseProgressListener listener) throws Exception;

    DetailedResponse rewrap(PackId packageId) throws Exception;

    DetailedResponse rewrap(PackId packageId, ResponseProgressListener listener) throws Exception;

    DetailedResponse uninstall(PackId packageId) throws Exception;

    DetailedResponse uninstall(PackId packageId, ResponseProgressListener listener) throws Exception;

}
