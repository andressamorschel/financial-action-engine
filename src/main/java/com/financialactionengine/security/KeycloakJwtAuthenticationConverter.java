package com.financialactionengine.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class KeycloakJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLIENT_ID = "financial-action-agent";

    private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities =
                defaultAuthoritiesConverter.convert(jwt);

        var combinedAuthorities = new java.util.ArrayList<GrantedAuthority>();

        Optional.ofNullable(authorities)
                .ifPresent(combinedAuthorities::addAll);

        Map<String, Object> resourceAccess =
                jwt.getClaimAsMap("resource_access");

        if (resourceAccess != null) {
            Object clientAccess = resourceAccess.get(CLIENT_ID);

            if (clientAccess instanceof Map<?, ?> clientDetails) {
                Object roles = clientDetails.get("roles");

                if (roles instanceof List<?> roleList) {
                    roleList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(combinedAuthorities::add);
                }
            }
        }

        return new JwtAuthenticationToken(jwt, combinedAuthorities);
    }
}
