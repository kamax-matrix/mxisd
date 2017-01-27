package io.kamax.mxisd.lookup

import io.kamax.mxisd.api.ThreePidType

interface ThreePidProvider {

    /**
     * Higher has more priority
     */
    int getPriority() // Should not be here but let's KISS for now

    Optional<?> find(ThreePidType type, String threePid)

}
