package io.kamax.mxisd.lookup

import io.kamax.mxisd.api.ThreePidType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PriorityLookupStrategy implements LookupStrategy {

    @Autowired
    private List<ThreePidProvider> providers

    @Override
    Optional<?> find(ThreePidType type, String threePid) {
        if (ThreePidType.email != type) {
            throw new IllegalArgumentException("${type} is currently not supported")
        }

        providers.sort(new Comparator<ThreePidProvider>() {

            @Override
            int compare(ThreePidProvider o1, ThreePidProvider o2) {
                return Integer.compare(o2.getPriority(), o1.getPriority())
            }

        })

        for (ThreePidProvider provider : providers) {
            Optional<?> lookupDataOpt = provider.find(type, threePid)
            if (lookupDataOpt.isPresent()) {
                return lookupDataOpt
            }
        }

        return Optional.empty()
    }

}
