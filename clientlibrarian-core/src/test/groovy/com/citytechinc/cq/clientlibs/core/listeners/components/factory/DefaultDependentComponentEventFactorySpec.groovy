package com.citytechinc.cq.clientlibs.core.listeners.components.factory

import com.citytechinc.aem.prosper.specs.ProsperSpec
import com.citytechinc.cq.clientlibs.api.events.components.factory.DependentComponentEventFactory
import com.citytechinc.cq.clientlibs.core.listeners.components.factory.impl.DefaultDependentComponentEventFactory

import javax.jcr.observation.Event

class DefaultDependentComponentEventFactorySpec extends ProsperSpec {

    def DependentComponentEventFactory factory = new DefaultDependentComponentEventFactory()

    def setupSpec() {

        nodeBuilder.apps {
            tacodan("sling:Folder") {
                components("sling:Folder") {
                    content("sling:Folder") {
                        chalupa("cq:Component", "jcr:title" : "Challupa", "dependencies" : "tacodan.component.chalupa")
                        gordita("cq:Component", "jcr:title" : "Gordita")
                    }
                }
            }
        }

    }

    def "Component Added with a Client Library dependency should elicit a New Component event"() {

        when: "A New Node event is issued for a new Component Node with Client Library dependencies"
        def newComponentNodeEvent = Mock(Event)
        newComponentNodeEvent.getPath() >> "/apps/tacodan/components/content/chalupa"
        newComponentNodeEvent.getType() >> Event.NODE_ADDED

        then: "The Component Factory to product a DependentComponentNode event"
        factory.make(newComponentNodeEvent, [:], session).isPresent()

    }

}
