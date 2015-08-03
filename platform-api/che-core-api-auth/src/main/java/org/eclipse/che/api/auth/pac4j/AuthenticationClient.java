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

import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.DirectClient;
import org.pac4j.core.client.Mechanism;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.CommonProfile;

/**
 * @author Sergii Kabashniuk
 */
public class AuthenticationClient extends DirectClient {

    @Override
    protected BaseClient newClient() {
        return new AuthenticationClient();
    }

    @Override
    protected CommonProfile retrieveUserProfile(Credentials credentials, WebContext context) {
        return null;
    }

    @Override
    public Mechanism getMechanism() {
        return Mechanism.PARAMETER_MECHANISM;
    }

    @Override
    public Credentials getCredentials(WebContext context) throws RequiresHttpAction {
        return null;
    }

    @Override
    protected void internalInit() {

    }
}
