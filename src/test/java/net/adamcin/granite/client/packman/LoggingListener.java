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

import org.slf4j.Logger;

/**
 * Created by madamcin on 3/17/14.
 */
public class LoggingListener extends DefaultResponseProgressListener {

    public enum Level {
        INFO, DEBUG
    }

    private final Logger logger;
    private final Level level;

    public LoggingListener(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    public void onStart(String title) {
        log(String.format("onStart[title=%s]", title));
    }

    public void onLog(String message) {
        log(String.format("onLog[message=%s]", message));
    }

    public void onMessage(String message) {
        log(String.format("onMessage[message=%s]", message));
    }

    public void onProgress(String action, String path) {
        log(String.format("onProgress[action=%s,path=%s]", action, path));
    }

    public void onError(String path, String error) {
        log(String.format("onError[path=%s,error=%s]", path, error));
    }

    private void log(String message) {
        switch (level) {
            case DEBUG:
                logger.debug(message);
                break;
            case INFO:
                logger.info(message);
                break;
        }
    }
}
