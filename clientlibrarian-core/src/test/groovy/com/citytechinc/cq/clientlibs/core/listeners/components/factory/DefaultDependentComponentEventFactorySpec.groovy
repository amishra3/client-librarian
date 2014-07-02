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
package com.citytechinc.cq.clientlibs.core.listeners.components.factory

import com.citytechinc.aem.prosper.specs.ProsperSpec
import com.citytechinc.cq.clientlibs.api.domain.component.DependentComponent
import com.citytechinc.cq.clientlibs.api.events.components.factory.DependentComponentEventFactory
import com.citytechinc.cq.clientlibs.core.listeners.components.factory.impl.DefaultDependentComponentEventFactory

import javax.jcr.observation.Event

class DefaultDependentComponentEventFactorySpec extends ProsperSpec {

    def factory = new DefaultDependentComponentEventFactory()
    def dependentComponentByPathMap = ["/apps/tacodan/components/content/mexicanpizza" : Mock(DependentComponent)]

    def setupSpec() {

        nodeBuilder.apps {
            tacodan("sling:Folder") {
                components("sling:Folder") {
                    content("sling:Folder") {
                        chalupa("cq:Component", "jcr:title" : "Challupa", "dependencies" : "tacodan.component.chalupa")
                        gordita("cq:Component", "jcr:title" : "Gordita")
                        mexicanpizza("cq:Component", "jcr:title" : "Mexican Pizza", "dependencies" : "tacodan.component.mexicanpizza")
                    }
                }
            }
        }

    }

    /*
     * Node Added Testing
     */

    def "Component Added with a Client Library dependency should elicit a New Component event"() {

        when: "A New Node event is issued for a new Component Node with Client Library dependencies"
        def newComponentNodeEvent = Mock(Event)
        newComponentNodeEvent.getPath() >> "/apps/tacodan/components/content/chalupa"
        newComponentNodeEvent.getType() >> Event.NODE_ADDED

        then: "The Component Factory to product a DependentComponentNode event"
        factory.make(newComponentNodeEvent, dependentComponentByPathMap, session).isPresent()

    }

    /*
     * Node Removed Testing
     */

    def "Component Removed with a Client Library dependency should elicit a RemovedDependentComponentEvent"() {

        when: "An existing Component with a Client Library dependency is removed"
        def componentRemovalEvent = Mock(Event)
        componentRemovalEvent.getPath() >> "/apps/tacodan/components/content/mexicanpizza"
        componentRemovalEvent.getType() >> Event.NODE_REMOVED

        then: "The factory should produce a RemovedDependentComponentEvent"
        factory.make(componentRemovalEvent, dependentComponentByPathMap, session).isPresent()
    }

    /*
     * Property Added Testing
     */

    def "Property Added event for a dependencies property on a Component node should elicit a NewDependentComponentEvent"() {

        when: "A dependencies property is added to a Component node"
        def propertyAddedEvent = Mock(Event)
        propertyAddedEvent.getPath() >> "/apps/tacodan/components/content/chalupa/dependencies"
        propertyAddedEvent.getType() >> Event.PROPERTY_ADDED

        then: "The factory should produce a NewDependentComponentEvent"
        factory.make(propertyAddedEvent, dependentComponentByPathMap, session).isPresent()

    }

    /*
     * Property Changed Testing
     */

    def "Property Changed event for a dependencies property on a Component node should elicit a ModifiedDependentComponentEvent"() {

        when: "A dependencies property of a Component node is changed"
        def propertyChangedEvent = Mock(Event)
        propertyChangedEvent.getPath() >> "/apps/tacodan/components/content/mexicanpizza/dependencies"
        propertyChangedEvent.getType() >> Event.PROPERTY_CHANGED

        then: "The factory should produce a ModifiedDependentComponentEvent"
        factory.make(propertyChangedEvent, dependentComponentByPathMap, session).isPresent()

    }

    /*
     * Property Removal Testing
     */

    def "Property Removed event for a dependencies property on a Component node should elicit a RemovedDependentComponentEvent"() {

        when: "A dependencies property of a Component node is removed"
        def propertyRemovedEvent = Mock(Event)
        propertyRemovedEvent.getPath() >> "/apps/tacodan/components/content/mexicanpizza/dependencies"
        propertyRemovedEvent.getType() >> Event.PROPERTY_REMOVED

        then: "The factory should produce a RemovedDependentComponentEvent"
        factory.make(propertyRemovedEvent, dependentComponentByPathMap, session).isPresent()

    }

    /*
     * Persist Event Testing
     */

    def "Persist event should always elicit a PersistEvent"() {

        when: "A persist event occurs"
        def persistEvent = Mock(Event)
        persistEvent.getPath() >> "/apps/tacodan"
        persistEvent.getType() >> Event.PERSIST

        then: "The factory should produce a PersistEvent"
        factory.make(persistEvent, dependentComponentByPathMap, session).isPresent()

    }

}
