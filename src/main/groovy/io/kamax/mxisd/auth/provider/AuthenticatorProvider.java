package io.kamax.mxisd.auth.provider;

import io.kamax.mxisd.auth.UserAuthResult;

public interface AuthenticatorProvider {

    boolean isEnabled();

    UserAuthResult authenticate(String id, String password);

}
