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

import org.eclipse.che.api.auth.CookiesTokenExtractor;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.http.credentials.TokenCredentials;
import org.pac4j.http.credentials.extractor.Extractor;

import javax.inject.Inject;

/**
 * @author Sergii Kabashniuk
 */
public class TokenExtractor implements Extractor<TokenCredentials> {
    private final org.eclipse.che.api.auth.TokenExtractor extractor = new CookiesTokenExtractor();

    private final String clientName;

    @Inject
    public TokenExtractor(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public TokenCredentials extract(WebContext context) {
        return new TokenCredentials(extractor.getToken(((J2EContext)context).getRequest()), clientName);
    }
}
