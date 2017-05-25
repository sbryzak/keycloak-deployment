/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authorization.policy.provider.role;

import static org.keycloak.authorization.policy.provider.role.RolePolicyProviderFactory.getRoles;

import java.util.Map;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.policy.evaluation.Evaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class RolePolicyProvider implements PolicyProvider {

    @Override
    public void evaluate(Evaluation evaluation) {
        Policy policy = evaluation.getPolicy();
        Map<String, Object>[] roleIds = getRoles(policy);
        AuthorizationProvider authorizationProvider = evaluation.getAuthorizationProvider();
        RealmModel realm = authorizationProvider.getKeycloakSession().getContext().getRealm();

        if (roleIds.length > 0) {
            Identity identity = evaluation.getContext().getIdentity();

            for (Map<String, Object> current : roleIds) {
                RoleModel role = realm.getRoleById((String) current.get("id"));

                if (role != null) {
                    boolean hasRole = hasRole(identity, role, realm);

                    if (!hasRole && Boolean.valueOf(isRequired(current))) {
                        evaluation.deny();
                        return;
                    } else if (hasRole) {
                        evaluation.grant();
                    }
                }
            }
        }
    }

    private boolean isRequired(Map<String, Object> current) {
        return (boolean) current.getOrDefault("required", false);
    }

    private boolean hasRole(Identity identity, RoleModel role, RealmModel realm) {
        String roleName = role.getName();
        if (role.isClientRole()) {
            ClientModel clientModel = realm.getClientById(role.getContainerId());
            return identity.hasClientRole(clientModel.getClientId(), roleName);
        }
        return identity.hasRealmRole(roleName);
    }

    @Override
    public void close() {

    }
}