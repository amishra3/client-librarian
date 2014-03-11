package com.citytechinc.cq.clientlibs.core.domain.sling.runmode;

import com.citytechinc.cq.clientlibs.api.domain.sling.runmode.SlingRunModeGroup;
import com.citytechinc.cq.clientlibs.core.domain.sling.runmode.impl.BasicSlingRunModeGroup;
import com.google.common.collect.Sets;

public class SlingRunModeGroups {

    /**
     * Produces a SlingRunMode object for a composite run mode string.  A composite run mode string
     * is a period delimited set of run modes.  Such a string may contain 0 or more run modes.
     *
     * @param compositeRunMode
     * @return
     */
    public static SlingRunModeGroup forCompositeRunMode(String compositeRunMode) {

        return new BasicSlingRunModeGroup(Sets.newHashSet(compositeRunMode.split("\\.")));
    }

}
