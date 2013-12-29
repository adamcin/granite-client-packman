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

package net.adamcin.granite.client.packman.async;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Cookie;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Realm;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import com.ning.http.multipart.FilePart;
import net.adamcin.granite.client.packman.AbstractPackageManagerClient;
import net.adamcin.granite.client.packman.DetailedResponse;
import net.adamcin.granite.client.packman.DownloadResponse;
import net.adamcin.granite.client.packman.ListResponse;
import net.adamcin.granite.client.packman.PackId;
import net.adamcin.granite.client.packman.ResponseProgressListener;
import net.adamcin.granite.client.packman.SimpleResponse;
import net.adamcin.granite.client.packman.UnauthorizedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class AsyncPackageManagerClient extends AbstractPackageManagerClient {
    private static final AsyncCompletionHandler<SimpleResponse> SIMPLE_RESPONSE_HANDLER =
            new AsyncCompletionHandler<SimpleResponse>() {
                @Override public SimpleResponse onCompleted(Response response) throws Exception {
                    return AbstractPackageManagerClient.parseSimpleResponse(
                            response.getStatusCode(),
                            response.getStatusText(),
                            response.getResponseBodyAsStream(),
                            getResponseEncoding(response)
                    );
                }
            };

    private static final AsyncCompletionHandler<ListResponse> LIST_RESPONSE_HANDLER =
            new AsyncCompletionHandler<ListResponse>() {
                @Override public ListResponse onCompleted(Response response) throws Exception {
                    return AbstractPackageManagerClient.parseListResponse(
                            response.getStatusCode(),
                            response.getStatusText(),
                            response.getResponseBodyAsStream(),
                            getResponseEncoding(response)
                    );
                }
            };

    private static class DownloadResponseHandler extends AsyncCompletionHandler<DownloadResponse> {
        final File outputFile;

        private DownloadResponseHandler(File outputFile) {
            this.outputFile = outputFile;
        }

        @Override public DownloadResponse onCompleted(Response response) throws Exception {
            return AbstractPackageManagerClient.parseDownloadResponse(
                    response.getStatusCode(),
                    response.getStatusText(),
                    response.getResponseBodyAsStream(),
                    this.outputFile);
        }
    }

    private final AsyncCompletionHandler<Response> AUTHORIZED_RESPONSE_HANDLER =
            new AuthorizedResponseHandler<Response>() {
                @Override protected Response onAuthorized(Response response) throws Exception {
                    return response;
                }
            };

    private final AsyncCompletionHandler<Response> ANY_RESPONSE_HANDLER =
            new AsyncCompletionHandler<Response>() {
                @Override
                public Response onCompleted(Response response) throws Exception {
                    return response;
                }
            };

    private final AsyncHttpClient client;

    private final List<Cookie> cookies = new ArrayList<Cookie>();

    private Realm realm = null;

    public AsyncPackageManagerClient() {
        this(new AsyncHttpClient());
    }

    public AsyncPackageManagerClient(final AsyncHttpClient client) {
        if (client == null) {
            throw new NullPointerException("client cannot be null");
        }
        this.client = client;
    }

    public AsyncHttpClient getClient() {
        return this.client;
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    public Realm getRealm() {
        return this.realm;
    }

    private void setCookies(Collection<Cookie> cookies) {
        this.cookies.clear();

        if (cookies != null) {
            this.cookies.addAll(cookies);
        }
    }

    @Override
    public boolean login(String username, String password) throws IOException {
        Request request = getClient().preparePost(getBaseUrl() + LOGIN_PATH)
                .addParameter(LOGIN_PARAM_USERNAME, username)
                .addParameter(LOGIN_PARAM_PASSWORD, password)
                .addParameter(LOGIN_PARAM_VALIDATE, LOGIN_VALUE_VALIDATE)
                .addParameter(LOGIN_PARAM_CHARSET, LOGIN_VALUE_CHARSET).build();
        try {
            ListenableFuture<Response> fResponse = getClient().executeRequest(request);
            Response response = getRequestTimeout() >= 0L ?
                    fResponse.get(getRequestTimeout(), TimeUnit.MILLISECONDS) : fResponse.get();

            if (response.getStatusCode() == 200) {
                this.setCookies(response.getCookies());
                return true;
            } else if (response.getStatusCode() == 405) {
                // fallback to legacy in case of 405 response
                return loginLegacy(username, password);
            }
        } catch (Exception e) {
            throw new IOException("Failed to login with provided credentials");
        }

        return false;
    }

    private boolean loginLegacy(String username, String password) throws IOException {
        Request request = getClient().preparePost(getBaseUrl() + LEGACY_PATH)
                .addParameter(LEGACY_PARAM_USERID, username)
                .addParameter(LEGACY_PARAM_PASSWORD, password)
                .addParameter(LEGACY_PARAM_WORKSPACE, LEGACY_VALUE_WORKSPACE)
                .addParameter(LEGACY_PARAM_TOKEN, LEGACY_VALUE_TOKEN)
                .addParameter(LOGIN_PARAM_CHARSET, LOGIN_VALUE_CHARSET).build();
        try {
            ListenableFuture<Response> fResponse = getClient().executeRequest(request);
            Response response = getRequestTimeout() >= 0L ?
                    fResponse.get(getRequestTimeout(), TimeUnit.MILLISECONDS) : fResponse.get();

            if (response.getStatusCode() == 200) {
                this.setCookies(response.getCookies());
                return true;
            }
        } catch (Exception e) {
            throw new IOException("Failed to login with provided credentials");
        }

        return false;
    }

    private ListenableFuture<Response> executeAnyRequest(Request request) throws IOException {
        return this.client.executeRequest(request, ANY_RESPONSE_HANDLER);
    }

    private ListenableFuture<Response> executeRequest(Request request) throws IOException {
        return this.client.executeRequest(request, AUTHORIZED_RESPONSE_HANDLER);
    }

    private SimpleResponse executeSimpleRequest(Request request)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        ListenableFuture<SimpleResponse> fResponse = this.client.executeRequest(request, SIMPLE_RESPONSE_HANDLER);
        return getRequestTimeout() >= 0L ? fResponse.get(getRequestTimeout(), TimeUnit.MILLISECONDS) : fResponse.get();
    }

    private DetailedResponse executeDetailedRequest(final Request request, final ResponseProgressListener listener)
        throws IOException, InterruptedException, ExecutionException, TimeoutException {

        ListenableFuture<DetailedResponse> fResponse = this.client.executeRequest(request, new AsyncCompletionHandler<DetailedResponse>(){
            @Override public DetailedResponse onCompleted(Response response) throws Exception {
                return AbstractPackageManagerClient.parseDetailedResponse(
                        response.getStatusCode(),
                        response.getStatusText(),
                        response.getResponseBodyAsStream(),
                        getResponseEncoding(response),
                        listener
                );
            }
        });

        return getRequestTimeout() >= 0L ? fResponse.get(getRequestTimeout(), TimeUnit.MILLISECONDS) : fResponse.get();
    }

    private ListResponse executeListRequest(Request request)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        ListenableFuture<ListResponse> fResponse = this.client.executeRequest(request, LIST_RESPONSE_HANDLER);
        return getRequestTimeout() >= 0L ? fResponse.get(getRequestTimeout(), TimeUnit.MILLISECONDS) : fResponse.get();
    }

    private DownloadResponse executeDownloadRequest(Request request, File outputFile)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        ListenableFuture<DownloadResponse> fResponse = this.client.executeRequest(request, new DownloadResponseHandler(outputFile));
        return getRequestTimeout() >= 0L ? fResponse.get(getRequestTimeout(), TimeUnit.MILLISECONDS) : fResponse.get();
    }

    private AsyncHttpClient.BoundRequestBuilder addContext(AsyncHttpClient.BoundRequestBuilder builder) {
        if (builder != null) {
            if (this.realm != null) {
                builder.setRealm(this.realm);
            }
            for (Cookie cookie : this.cookies) {
                builder.addCookie(cookie);
            }
        }
        return builder;
    }

    /**
     * {@inheritDoc}
     */
    protected final Either<? extends Exception, Boolean> checkServiceAvailability(final boolean checkTimeout,
                                                                                  final long timeoutRemaining) {
        final Request request = this.addContext(this.client.prepareGet(getJsonUrl())).build();

        try {
            final ListenableFuture<Response> future = executeAnyRequest(request);

            Response response = null;
            if (checkTimeout) {
                response = future.get(timeoutRemaining, TimeUnit.MILLISECONDS);
            } else {
                response = future.get();
            }

            if (response.getStatusCode() == 401) {
                return left(new UnauthorizedException("401 Unauthorized. Please login."), Boolean.class);
            } else {
                return right(Exception.class, response.getStatusCode() == 405);
            }
        } catch (TimeoutException e) {
            return left(new IOException("Service timeout exceeded."), Boolean.class);
        } catch (Exception e) {
            return left(e, Boolean.class);
        }
    }

    private AsyncHttpClient.BoundRequestBuilder buildSimpleRequest(PackId packageId) {
        if (packageId != null) {
            return this.addContext(this.client.preparePost(getJsonUrl(packageId)));
        } else {
            return this.addContext(this.client.preparePost(getJsonUrl()));
        }
    }

    private AsyncHttpClient.BoundRequestBuilder buildDetailedRequest(PackId packageId) {
        if (packageId != null) {
            return this.addContext(this.client.preparePost(getHtmlUrl(packageId)));
        } else {
            return this.addContext(this.client.preparePost(getHtmlUrl()));
        }
    }

    private AsyncHttpClient.BoundRequestBuilder buildListRequest() {
        return this.addContext(this.client.prepareGet(getListUrl()));
    }

    private AsyncHttpClient.BoundRequestBuilder buildDownloadRequest() {
        return this.addContext(this.client.prepareGet(getDownloadUrl()));
    }

    private static String getResponseEncoding(Response response) {
        String encoding = response.getHeader("Content-Encoding");

        if (encoding == null) {
            String contentType = response.getContentType();
            if (contentType != null) {
                int charsetBegin = contentType.toLowerCase().indexOf(";charset=");
                if (charsetBegin >= 0) {
                    encoding = contentType.substring(charsetBegin + ";charset=".length());
                }
            }
        }

        return encoding;
    }

    abstract class AuthorizedResponseHandler<T> extends AsyncCompletionHandler<T> {
        protected abstract T onAuthorized(Response response) throws Exception;

        @Override
        public final T onCompleted(Response response) throws Exception {
            if (response.getStatusCode() == 401) {
                throw new UnauthorizedException(Integer.toString(response.getStatusCode()) + " " + response.getStatusText());
            } else {
                return onAuthorized(response);
            }
        }

        @Override
        public void onThrowable(Throwable t) {
            // do nothing
        }
    }

    @Override
    protected ResponseBuilder getResponseBuilder() {
        return new AsyncResponseBuilder();
    }

    class AsyncResponseBuilder extends ResponseBuilder {

        private PackId packId = null;
        private Map<String, String> stringParams = new HashMap<String, String>();
        private Map<String, FilePart> fileParams = new HashMap<String, FilePart>();

        @Override
        protected ResponseBuilder forPackId(PackId packId) {
            this.packId = packId;
            return this;
        }

        @Override
        public ResponseBuilder withParam(String name, String value) {
            this.stringParams.put(name, value);
            return this;
        }

        @Override
        public ResponseBuilder withParam(String name, boolean value) {
            return this.withParam(name, Boolean.toString(value));
        }

        @Override
        public ResponseBuilder withParam(String name, int value) {
            return this.withParam(name, Integer.toString(value));
        }

        @Override
        public ResponseBuilder withParam(String name, File value, String mimeType) throws IOException {
            this.fileParams.put(name, new FilePart(name, value, mimeType, null));
            return this;
        }

        @Override
        public SimpleResponse getSimpleResponse() throws Exception {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = buildSimpleRequest(packId);
            for (Map.Entry<String, String> param : this.stringParams.entrySet()) {
                if (this.fileParams.isEmpty()) {
                    requestBuilder.addParameter(param.getKey(), param.getValue());
                } else {
                    requestBuilder.addQueryParameter(param.getKey(), param.getValue());
                }
            }

            for (Map.Entry<String, FilePart> param : this.fileParams.entrySet()) {
                requestBuilder.addBodyPart(param.getValue());
            }

            return executeSimpleRequest(requestBuilder.build());
        }

        @Override
        public DetailedResponse getDetailedResponse(final ResponseProgressListener listener) throws Exception {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = buildDetailedRequest(packId);
            for (Map.Entry<String, String> param : this.stringParams.entrySet()) {
                if (this.fileParams.isEmpty()) {
                    requestBuilder.addParameter(param.getKey(), param.getValue());
                } else {
                    requestBuilder.addQueryParameter(param.getKey(), param.getValue());
                }
            }

            for (Map.Entry<String, FilePart> param : this.fileParams.entrySet()) {
                requestBuilder.addBodyPart(param.getValue());
            }

            return executeDetailedRequest(requestBuilder.build(), listener);
        }

        @Override
        protected ListResponse getListResponse() throws Exception {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = buildListRequest();
            if (packId != null) {
                requestBuilder.addQueryParameter(KEY_PATH, packId.getInstallationPath() + ".zip");
            }

            for (Map.Entry<String, String> param : this.stringParams.entrySet()) {
                requestBuilder.addQueryParameter(param.getKey(), param.getValue());
            }

            return executeListRequest(requestBuilder.build());
        }

        @Override
        protected DownloadResponse getDownloadResponse(File file) throws Exception {
            AsyncHttpClient.BoundRequestBuilder requestBuilder = buildDownloadRequest();
            if (packId != null) {
                requestBuilder.addQueryParameter(KEY_PATH, packId.getInstallationPath() + ".zip");
            }

            for (Map.Entry<String, String> param : this.stringParams.entrySet()) {
                requestBuilder.addQueryParameter(param.getKey(), param.getValue());
            }

            return executeDownloadRequest(requestBuilder.build(), file);
        }
    }
}
