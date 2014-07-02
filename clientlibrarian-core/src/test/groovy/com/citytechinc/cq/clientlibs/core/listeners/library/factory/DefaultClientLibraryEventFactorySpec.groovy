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
package com.citytechinc.cq.clientlibs.core.listeners.library.factory

import com.citytechinc.aem.prosper.specs.ProsperSpec
import com.citytechinc.cq.clientlibs.api.domain.library.ClientLibrary
import com.citytechinc.cq.clientlibs.api.events.components.factory.DependentComponentEventFactory
import com.citytechinc.cq.clientlibs.api.events.library.factory.ClientLibraryEventFactory
import com.citytechinc.cq.clientlibs.core.listeners.components.factory.impl.DefaultDependentComponentEventFactory
import com.citytechinc.cq.clientlibs.core.listeners.library.factory.impl.DefaultClientLibraryEventFactory
import javax.jcr.Node as JcrNode
import javax.jcr.Property
import javax.jcr.observation.Event

/**
 * Created with IntelliJ IDEA.
 * User: paulmichelotti
 * Date: 4/23/14
 * Time: 5:04 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultClientLibraryEventFactorySpec extends ProsperSpec {

    def factory = new DefaultClientLibraryEventFactory()
    def clientLibraryByPathMap = [
            "/etc/clientlibs/tacodan/oldlib" : Mock(ClientLibrary)
    ]

    def setupSpec() {

        nodeBuilder.etc {
            clientlibs("sling:Folder") {
                tacodan("sling:Folder") {
                    newlib("cq:ClientLibraryFolder", "categories" : "tacodan.newlib") {
                        "js.txt"("nt:file") {
                            "jcr:content"("nt:resource", "jcr:mimeType" : "text/plain", "jcr:data" : "somejs.js")
                        }
                        "css.txt"("nt:file") {
                            "jcr:content"("nt:resource", "jcr:mimeType" : "text/plain", "jcr:data" : "somecss.css")
                        }
                    }
                    oldlib("cq:ClientLibraryFolder", "categories" : "tacodan.oldlib", "dependencies" : "tacodan.tacolib", "embed" : "tacodan.embedlib", "runModes" : "publish") {
                        "js.txt"("nt:file") {
                            "jcr:content"("nt:resource", "jcr:mimeType" : "text/plain", "jcr:data" : "someoldjs.js")
                        }
                        "css.txt"("nt:file") {
                            "jcr:content"("nt:resource", "jcr:mimeType" : "text/plain", "jcr:data" : "someoldcss.css")
                        }
                    }
                }
            }
        }

    }

    /*
     * Node Added Testing
     */

    def "js.txt file addition should elicit a JsLibraryAdditionEvent"() {

        when: "A new node event is issued for a js.txt"
        def newJsTxtNodeEvent = Mock(Event)
        newJsTxtNodeEvent.getPath() >> "/etc/clientlibs/tacodan/newlib/js.txt"
        newJsTxtNodeEvent.getType() >> Event.NODE_ADDED

        then: "the factory should produce a JsLibraryAdditionEvent"
        factory.make(newJsTxtNodeEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "css.txt file addition should elicit a CssLibraryAdditionEvent"() {

        when: "A new node event is issued for a css.txt"
        def newCssTxtNodeEvent = Mock(Event)
        newCssTxtNodeEvent.getPath() >> "/etc/clientlibs/tacodan/newlib/css.txt"
        newCssTxtNodeEvent.getType() >> Event.NODE_ADDED

        then: "the factory should produce a CssLibraryAdditionEvent"
        factory.make(newCssTxtNodeEvent, clientLibraryByPathMap, session).isPresent()

    }

    /*
     * Node Removal Testing
     */

    def "js.txt file removal from known library should elicit a JsLibraryRemovalEvent"() {

        when: "A js.txt node is removed from a known library"
        def removeJsTxtNodeEvent = Mock(Event)
        removeJsTxtNodeEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/js.txt"
        removeJsTxtNodeEvent.getType() >> Event.NODE_REMOVED

        then: "the factory should produce a JsLibraryRemovalEvent"
        factory.make(removeJsTxtNodeEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "css.txt file removal from known library should elicit a CssLibraryRemovalEvent"() {

        when: "A css.txt node is removed from a known library"
        def removeCssTxtNodeEvent = Mock(Event)
        removeCssTxtNodeEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/css.txt"
        removeCssTxtNodeEvent.getType() >> Event.NODE_REMOVED

        then: "the factory should produce a CssLibraryRemovalEvent"
        factory.make(removeCssTxtNodeEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Removal of a Client Library node for a known library should elicit a ClientLibraryRemovalEvent"() {

        when: "A Client Library node for a known library is removed"
        def removeClientLibraryNodeEvent = Mock(Event)
        removeClientLibraryNodeEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib"
        removeClientLibraryNodeEvent.getType() >> Event.NODE_REMOVED

        then: "the factory should produce a ClientLibraryRemovalEvent"
        factory.make(removeClientLibraryNodeEvent, clientLibraryByPathMap, session).isPresent()

    }

    /*
     * Property Addition Testing
     */

    def "Property addition event for a categories property on a Client Library node should elicit a NewClientLibraryEvent"() {

        when: "A Property addition event is issued for a categories property on a Client Library node"
        def categoryPropertyAdditionEvent = Mock(Event)
        categoryPropertyAdditionEvent.getPath() >> "/etc/clientlibs/tacodan/newlib/categories"
        categoryPropertyAdditionEvent.getType() >> Event.PROPERTY_ADDED

        then: "the factory should produce a NewClientLibraryEvent"
        factory.make(categoryPropertyAdditionEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property addition event for a dependencies property on a Client Library node should elicit a ClientLibraryDependencyModificationEvent"() {

        when: "A Property addition event is issued for a dependencies property on a Client Library node"
        def dependenciesPropertyAdditionEvent = Mock(Event)
        dependenciesPropertyAdditionEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/dependencies"
        dependenciesPropertyAdditionEvent.getType() >> Event.PROPERTY_ADDED

        then: "the factory should produce a ClientLibraryDependencyModificationEvent"
        factory.make(dependenciesPropertyAdditionEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property addition event for an embed property on a Client Library node should elicit a ClientLibraryEmbedsModificationEvent"() {

        when: "A Property addition event is issued for an embed property on a Client Library node"
        def embedsPropertyAdditionEvent = Mock(Event)
        embedsPropertyAdditionEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/embed"
        embedsPropertyAdditionEvent.getType() >> Event.PROPERTY_ADDED

        then: "the factory should produce a ClientLibraryEmbedsModificationEvent"
        factory.make(embedsPropertyAdditionEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property addition event for a runModes property on a Client Library node should elicit a ClientLibraryRunModesModificationEvent"() {

        when: "A Property addition event is issued for a runModes property on a Client Library node"
        def runModesPropertyAdditionEvent = Mock(Event)
        runModesPropertyAdditionEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/runModes"
        runModesPropertyAdditionEvent.getType() >> Event.PROPERTY_ADDED

        then: "the factory should produce a ClientLibraryRunModesModificationEvent"
        factory.make(runModesPropertyAdditionEvent, clientLibraryByPathMap, session).isPresent()

    }

    /*
     * Property Modification Testing
     */
    def "Property change event for a categories property on a Client Library node should elicit a ClientLibraryCategoriesModificationEvent"() {

        when: "A Property change event is issued for a categories property on a Client Library node"
        def categoryPropertyChangeEvent = Mock(Event)
        categoryPropertyChangeEvent.getPath() >> "/etc/clientlibs/tacodan/newlib/categories"
        categoryPropertyChangeEvent.getType() >> Event.PROPERTY_CHANGED

        then: "the factory should produce a ClientLibraryCategoriesModificationEvent"
        factory.make(categoryPropertyChangeEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property change event for a dependencies property on a Client Library node should elicit a ClientLibraryDependencyModificationEvent"() {

        when: "A Property change event is issued for a dependencies property on a Client Library node"
        def dependenciesPropertyChangeEvent = Mock(Event)
        dependenciesPropertyChangeEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/dependencies"
        dependenciesPropertyChangeEvent.getType() >> Event.PROPERTY_CHANGED

        then: "the factory should produce a ClientLibraryDependencyModificationEvent"
        factory.make(dependenciesPropertyChangeEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property change event for an embed property on a Client Library node should elicit a ClientLibraryEmbedsModificationEvent"() {

        when: "A Property change event is issued for an embed property on a Client Library node"
        def embedsPropertyChangeEvent = Mock(Event)
        embedsPropertyChangeEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/embed"
        embedsPropertyChangeEvent.getType() >> Event.PROPERTY_CHANGED

        then: "the factory should produce a ClientLibraryEmbedsModificationEvent"
        factory.make(embedsPropertyChangeEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property change event for a runModes property on a Client Library node should elicit a ClientLibraryRunModesModificationEvent"() {

        when: "A Property change event is issued for a runModes property on a Client Library node"
        def runModesPropertyChangeEvent = Mock(Event)
        runModesPropertyChangeEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/runModes"
        runModesPropertyChangeEvent.getType() >> Event.PROPERTY_CHANGED

        then: "the factory should produce a ClientLibraryRunModesModificationEvent"
        factory.make(runModesPropertyChangeEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property change event for the jcr:data of a js.txt file of a Client Library node should elicit a JsLibraryModificationEvent"() {

        when: "A Property change event is issued for a jcr:data property on a js.txt file of a Client Library node"
        def dataPropertyChangeEvent = Mock(Event)
        dataPropertyChangeEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/js.txt/jcr:content/jcr:data"
        dataPropertyChangeEvent.getType() >> Event.PROPERTY_CHANGED

        then: "the factory should produce a JsLibraryModificationEvent"
        factory.make(dataPropertyChangeEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property change event for the jcr:data of a css.txt file of a Client Library node should elicit a CssLibraryModificationEvent"() {

        when: "A Property change event is issued for a jcr:data property on a css.txt file of a Client Library node"
        def dataPropertyChangeEvent = Mock(Event)
        dataPropertyChangeEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/css.txt/jcr:content/jcr:data"
        dataPropertyChangeEvent.getType() >> Event.PROPERTY_CHANGED

        then: "the factory should produce a CssLibraryModificationEvent"
        factory.make(dataPropertyChangeEvent, clientLibraryByPathMap, session).isPresent()

    }

    /*
     * Property Removal Testing
     */

    def "Property removal event for a categories property of a Client Library node should elicit a ClientLibraryRemovalEvent"() {

        when: "A Property removal event is issued for the categories property of a Client Library node"
        def propertyRemovalEvent = Mock(Event)
        propertyRemovalEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/categories"
        propertyRemovalEvent.getType() >> Event.PROPERTY_REMOVED

        then: "the factory should produce a ClientLibraryRemovalEvent"
        factory.make(propertyRemovalEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property removal event for a dependencies property of a Client Library node should elicit a ClientLibraryDependenciesModificationEvent"() {

        when: "A Property removal event is issued for the dependencies property of a Client Library node"
        def propertyRemovalEvent = Mock(Event)
        propertyRemovalEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/dependencies"
        propertyRemovalEvent.getType() >> Event.PROPERTY_REMOVED

        then: "the factory should produce a ClientLibraryDependenciesModificationEvent"
        factory.make(propertyRemovalEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property removal event for an embed property of a Client Library node should elicit a ClientLibraryEmbedsModificationEvent"() {

        when: "A Property removal event is issued for the embed property of a Client Library node"
        def propertyRemovalEvent = Mock(Event)
        propertyRemovalEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/embed"
        propertyRemovalEvent.getType() >> Event.PROPERTY_REMOVED

        then: "the factory should produce a ClientLibraryEmbedsModificationEvent"
        factory.make(propertyRemovalEvent, clientLibraryByPathMap, session).isPresent()

    }

    def "Property removal event for a runModes property of a Client Library node should elicit a ClientLibraryRunModesModificationEvent"() {

        when: "A Property removal event is issued for the runModes property of a Client Library node"
        def propertyRemovalEvent = Mock(Event)
        propertyRemovalEvent.getPath() >> "/etc/clientlibs/tacodan/oldlib/runModes"
        propertyRemovalEvent.getType() >> Event.PROPERTY_REMOVED

        then: "the factory should produce a ClientLibraryRunModesModificationEvent"
        factory.make(propertyRemovalEvent, clientLibraryByPathMap, session).isPresent()

    }

    /*
     * Persist Testing
     */

    def "A Persist event should always elicit a PersistEvent"() {

        when: "A Persist event is issued"
        def persistEvent = Mock(Event)
        persistEvent.getPath() >> "/etc/clientlibs"
        persistEvent.getType() >> Event.PERSIST

        then: "the factory should produce a PersistEvent"
        factory.make(persistEvent, clientLibraryByPathMap, session).isPresent()

    }

}
