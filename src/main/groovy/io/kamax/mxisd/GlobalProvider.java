package io.kamax.mxisd;

import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;

public interface GlobalProvider extends AuthenticatorProvider, IThreePidProvider {
}
