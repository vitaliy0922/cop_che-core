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
package org.eclipse.che.api.auth.pac4j;

import static com.google.common.collect.FluentIterable.from;
import static java.lang.Boolean.parseBoolean;

import com.google.common.base.Function;

import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.workspace.server.dao.MemberDao;
import org.pac4j.core.authorization.AuthorizationGenerator;
import org.pac4j.core.profile.CommonProfile;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Sergii Kabashniuk
 */
public class CheAuthorizationGenerator implements AuthorizationGenerator {

    @Inject
    UserDao       userDao;
    @Inject
    PreferenceDao preferenceDao;
    @Inject
    AccountDao    accountDao;
    @Inject
    MemberDao     memberDao;

    @Override
    public void generate(CommonProfile profile) {

        try {
            final List<String> userRoles = new ArrayList<>();
            String userId = profile.getId();
            final Map<String, String> preferences = preferenceDao.getPreferences(userId);
            if (parseBoolean(preferences.get("temporary"))) {
                userRoles.add("temp_user");
            } else {
                userRoles.add("user");
            }

            userRoles.addAll(from(accountDao.getByMember(userId))
                                           .transformAndConcat(new Function<Member, Iterable<String>>() {
                                               @Nullable
                                               @Override
                                               public Iterable<String> apply(final @Nullable Member member) {
                                                   return from(member.getRoles()).transform(new Function<String, String>() {
                                                       @Nullable
                                                       @Override
                                                       public String apply(@Nullable String role) {
                                                           return "account/" + member.getAccountId() + "/" + role;
                                                       }
                                                   });
                                               }
                                           }).toList()
                            );

            userRoles.addAll(from(memberDao.getUserRelationships(userId))
                                           .transformAndConcat(
                                                   new Function<org.eclipse.che.api.workspace.server.dao.Member, Iterable<String>>() {
                                                       @Nullable
                                                       @Override
                                                       public Iterable<String> apply(
                                                               final @Nullable org.eclipse.che.api.workspace.server.dao.Member member) {
                                                           return from(member.getRoles())
                                                                                .transform(new Function<String, String>() {
                                                                                    @Nullable
                                                                                    @Override
                                                                                    public String apply(@Nullable String role) {
                                                                                        return "workspace/" + member.getWorkspaceId() +
                                                                                               "/" + role;
                                                                                    }
                                                                                });
                                                       }
                                                   }).toList()
                            );

        } catch (Exception e) {

        }

    }


}
