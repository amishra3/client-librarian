package com.citytechinc.cq.clientlibs.core.domain.sling.runmode.impl;

import com.citytechinc.cq.clientlibs.api.domain.sling.runmode.SlingRunModeGroup;

import java.util.Set;

public class BasicSlingRunModeGroup implements SlingRunModeGroup {

    private Set<String> containedRunModes;

    public BasicSlingRunModeGroup(Set<String> containedRunModes) {
        this.containedRunModes = containedRunModes;
    }

    @Override
    public Boolean matches(Set<String> runModes) {
        return runModes.containsAll(containedRunModes);
    }

    public Set<String> getContainedRunModes() {
        return containedRunModes;
    }
}
