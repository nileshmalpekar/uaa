/*******************************************************************************
 *     Cloud Foundry
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.authentication;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.identity.uaa.authentication.manager.UaaAuthenticationPrototype;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.EMPTY_MAP;

/**
 * Authentication token which represents a user.
 */
@JsonSerialize(using = UaaAuthenticationSerializer.class)
@JsonDeserialize(using = UaaAuthenticationDeserializer.class)
public class UaaAuthentication implements Authentication, Serializable {

    private List<? extends GrantedAuthority> authorities;
    private Object credentials;
    private UaaPrincipal principal;
    private UaaAuthenticationDetails details;
    private boolean authenticated;
    private long authenticatedTime = -1l;
    private long expiresAt = -1l;
    private Set<String> externalGroups;
    private Map<String, List<String>> userAttributes;

    /**
     * Creates a token with the supplied array of authorities.
     *
     * @param authorities the collection of <tt>GrantedAuthority</tt>s for the
     *                    principal represented by this authentication object.
     */
    public UaaAuthentication(UaaPrincipal principal,
                             List<? extends GrantedAuthority> authorities,
                             UaaAuthenticationDetails details) {
        this(UaaAuthenticationPrototype.alreadyAuthenticated()
                .withPrincipal(principal)
                .withDetails(details)
                .withAuthorities(authorities)
        );
    }

    public UaaAuthentication(UaaPrincipal principal,
                             Object credentials,
                             List<? extends GrantedAuthority> authorities,
                             UaaAuthenticationDetails details,
                             boolean authenticated,
                             long authenticatedTime) {
        this(UaaAuthenticationPrototype.notYetAuthenticated()
                .withPrincipal(principal)
                .withAuthenticated(authenticated)
                .withAuthenticatedTime(authenticatedTime)
                .withDetails(details)
                .withAuthorities(authorities)
                .withCredentials(credentials)
        );
    }
    
    public UaaAuthentication(UaaPrincipal principal,
                             Object credentials,
                             List<? extends GrantedAuthority> authorities,
                             UaaAuthenticationDetails details,
                             boolean authenticated,
                             long authenticatedTime,
                             long expiresAt) {
        this(UaaAuthenticationPrototype.notYetAuthenticated()
                .withPrincipal(principal)
                .withAuthenticated(authenticated)
                .withAuthenticatedTime(authenticatedTime)
                .withDetails(details)
                .withExpiresAt(expiresAt)
                .withAuthorities(authorities)
                .withCredentials(credentials)
        );
    }

    public UaaAuthentication(UaaPrincipal uaaPrincipal,
                             Object credentials,
                             List<? extends GrantedAuthority> uaaAuthorityList,
                             Set<String> externalGroups,
                             Map<String, List<String>> userAttributes,
                             UaaAuthenticationDetails details,
                             boolean authenticated,
                             long authenticatedTime,
                             long expiresAt) {
        this(UaaAuthenticationPrototype.notYetAuthenticated()
                .withPrincipal(uaaPrincipal)
                .withAuthenticated(authenticated)
                .withAuthenticatedTime(authenticatedTime)
                .withExternalGroups(externalGroups)
                .withDetails(details)
                .withExpiresAt(expiresAt)
                .withAuthorities(uaaAuthorityList)
                .withCredentials(credentials)
                .withAttributes(userAttributes)
        );
    }

    public UaaAuthentication(UaaAuthenticationPrototype prototype) {
        if (prototype.getPrincipal() == null || prototype.getAuthorities() == null) {
            throw new IllegalArgumentException("principal and authorities must not be null");
        }
        this.principal = prototype.getPrincipal();
        this.authorities = prototype.getAuthorities();
        this.details = prototype.getDetails();
        this.credentials = prototype.getCredentials();
        this.authenticated = prototype.isAuthenticated();
        this.authenticatedTime = prototype.getAuthenticatedTime();
        if (this.authenticatedTime <= 0) this.authenticatedTime = -1;
        this.expiresAt = prototype.getExpiresAt();
        if (this.expiresAt <= 0) this.expiresAt = -1;
        this.externalGroups = prototype.getExternalGroups();
        this.userAttributes = prototype.getAttributes();
    }

    public long getAuthenticatedTime() {
        return authenticatedTime;
    }

    @Override
    public String getName() {
        // Should we return the ID for the principal name? (No, because the
        // UaaUserDatabase retrieves users by name.)
        return principal.getName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getDetails() {
        return details;
    }

    @Override
    public UaaPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated && (expiresAt > 0 ? expiresAt > System.currentTimeMillis() : true);
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        authenticated = isAuthenticated;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UaaAuthentication that = (UaaAuthentication) o;

        if (!authorities.equals(that.authorities)) {
            return false;
        }
        if (!principal.equals(that.principal)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = authorities.hashCode();
        result = 31 * result + principal.hashCode();
        return result;
    }

    public Set<String> getExternalGroups() {
        return externalGroups;
    }

    public void setExternalGroups(Set<String> externalGroups) {
        this.externalGroups = externalGroups;
    }

    public MultiValueMap<String,String> getUserAttributes() {
        return new LinkedMultiValueMap<>(userAttributes!=null?userAttributes: EMPTY_MAP);
    }

    public Map<String,List<String>> getUserAttributesAsMap() {
        return userAttributes!=null ? new HashMap<>(userAttributes) : EMPTY_MAP;
    }

    public void setUserAttributes(MultiValueMap<String, String> userAttributes) {
        this.userAttributes = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : userAttributes.entrySet()) {
            this.userAttributes.put(entry.getKey(), entry.getValue());
        }
    }

}
