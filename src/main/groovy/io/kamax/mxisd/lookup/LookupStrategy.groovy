package io.kamax.mxisd.lookup

import io.kamax.mxisd.api.ThreePidType

interface LookupStrategy {

    Optional<?> find(ThreePidType type, String threePid)

}
