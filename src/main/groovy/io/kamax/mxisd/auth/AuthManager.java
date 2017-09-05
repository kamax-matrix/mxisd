package io.kamax.mxisd.auth;

import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthManager {

    private Logger log = LoggerFactory.getLogger(AuthManager.class);

    @Autowired
    private List<AuthenticatorProvider> providers = new ArrayList<>();

    public UserAuthResult authenticate(String id, String password) {
        for (AuthenticatorProvider provider : providers) {
            if (!provider.isEnabled()) {
                continue;
            }

            UserAuthResult result = provider.authenticate(id, password);
            if (result.isSuccess()) {
                return result;
            }
        }

        return new UserAuthResult().failure();
    }

}
