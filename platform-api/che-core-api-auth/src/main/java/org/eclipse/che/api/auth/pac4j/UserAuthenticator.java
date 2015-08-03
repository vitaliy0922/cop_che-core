/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2015] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package org.eclipse.che.api.auth.pac4j;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.pac4j.core.exception.CredentialsException;
import org.pac4j.http.credentials.UsernamePasswordCredentials;
import org.pac4j.http.credentials.authenticator.UsernamePasswordAuthenticator;

import javax.inject.Inject;

/**
 * @author Sergii Kabashniuk
 */
public class UserAuthenticator implements UsernamePasswordAuthenticator {

    private final UserDao userDao;

    @Inject
    public UserAuthenticator(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void validate(UsernamePasswordCredentials credentials) {
        try {
            userDao.authenticate(credentials.getUsername(), credentials.getPassword());
        } catch (NotFoundException | ServerException e) {
            throw new CredentialsException(e.getLocalizedMessage());
        }
    }
}
