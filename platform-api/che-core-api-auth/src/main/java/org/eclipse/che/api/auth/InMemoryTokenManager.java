/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;


import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Hold information about tokens in memory.
 *
 * @author Sergii Kabashniuk
 */
@Singleton
public class InMemoryTokenManager implements TokenManager {
    private final Cache<String, String> tokens;
    private final TokenGenerator        tokenGenerator;
    private final Map<String, String>   keys;


    @Inject
    public InMemoryTokenManager(final TokenGenerator tokenGenerator, final TokenInvalidationHandler invalidationHandler) {
        this.tokenGenerator = tokenGenerator;
        this.tokens = CacheBuilder.newBuilder().
                removalListener(new RemovalListener<String, String>() {

                    @Override
                    public void onRemoval(RemovalNotification<String, String> notification) {
                        invalidationHandler.onTokenInvalidated(notification.getValue());
                    }
                }).expireAfterWrite(3, TimeUnit.DAYS).build();
        this.keys = tokens.asMap();
    }

    @Override
    public String createToken(String userId) {


        try {
            return tokens.get(userId, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return tokenGenerator.generate();
                }
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }

    }

    @Override
    public String getUserId(String token) {
        return keys.get(token);
    }

    @Override
    public boolean isValid(String token) {
        return keys.containsKey(token);
    }

    @Override
    public void invalidateToken(String token) {
        invalidateUserToken(getUserId(token));
    }

    @Override
    public void invalidateUserToken(String userId) {
        tokens.invalidate(userId);
    }
}
