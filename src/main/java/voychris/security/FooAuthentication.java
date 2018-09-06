package voychris.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class FooAuthentication extends AbstractAuthenticationToken {

    private final String rawCredentials;

    public FooAuthentication(String username, String rawCredentials) {
        super(null);

        super.setDetails(username);

        this.rawCredentials = rawCredentials;

        super.setAuthenticated(false);
    }

    public FooAuthentication(String username, Collection<GrantedAuthority> authorities) {
        super(authorities);
        super.setDetails(username);

        this.rawCredentials = null;

        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return super.getDetails();
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }

        super.setAuthenticated(false);
    }

    public String getRawCredentials() {
        return rawCredentials;
    }
}
