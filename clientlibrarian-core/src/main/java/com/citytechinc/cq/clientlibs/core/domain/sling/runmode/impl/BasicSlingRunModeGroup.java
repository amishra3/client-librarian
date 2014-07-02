/**
 * Copyright 2014 CITYTECH, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
