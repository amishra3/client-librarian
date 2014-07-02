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
