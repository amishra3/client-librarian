package com.citytechinc.cq.clientlibs.api.domain.sling.runmode;

import java.util.Set;

public interface SlingRunModeGroup {

    /**
     *
     * @param runModes
     * @return True if the run modes represented by this group are equal to or a subset of the provided run modes
     */
    public Boolean matches(Set<String> runModes);

}
