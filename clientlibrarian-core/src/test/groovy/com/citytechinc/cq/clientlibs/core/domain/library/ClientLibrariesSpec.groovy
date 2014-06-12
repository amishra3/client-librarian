package com.citytechinc.cq.clientlibs.core.domain.library

import com.citytechinc.aem.prosper.specs.ProsperSpec

class ClientLibrariesSpec extends ProsperSpec {

    def setupSpec() {

        nodeBuilder.etc {
            clientlibs("sling:Folder") {
                tacodan("sling:Folder") {
                    tacolib("cq:ClientLibraryFolder", "categories" : "tacodan.tacolib", "embed" : "tacodan.salsalib", "dependencies" : "tacodan.tamalelib", "conditionalDependencies" : "tacodan.gorditalib", "runModes" : "publish", "brands" : "default") {
                        "js.txt"("nt:file") {
                            "jcr:content"("nt:resource", "jcr:mimeType" : "text/plain", "jcr:data" : "somejs.js")
                        }
                        "css.txt"("nt:file") {
                            "jcr:content"("nt:resource", "jcr:mimeType" : "text/plain", "jcr:data" : "somecss.css")
                        }
                    }
                    salsalib("cq:ClientLibraryFolder", "categories" : "tacodan.salsalib", "runModes" : "dev.east.publish") {
                        "js.txt"("nt:file") {
                            "jcr:content"("nt:resource", "jcr:mimeType" : "text/plain", "jcr:data" : "somejs.js")
                        }
                        "css.txt"("nt:file") {
                            "jcr:content"("nt:resource", "jcr:mimeType" : "text/plain", "jcr:data" : "somecss.css")
                        }
                    }
                    tamalelib("cq:ClientLibraryFolder", "categories" : [ "tacodan.tamalelib", "tacodan.taquitolib" ])
                    gorditalib("cq:ClientLibraryFolder", "categories" : "tacodan.gorditalib")
                }
            }
        }

    }

    def "Client Library Resource with a specified Category should result in a Client Library object with the same category"() {

        when: "A Client Library is built for a Library Node with a single Category"
        def clientLibrary = ClientLibraries.forResource(resourceResolver.getResource("/etc/clientlibs/tacodan/tacolib"))

        then: "The constructed instance should contain the single category"
        clientLibrary.categories.size().equals(1)
        clientLibrary.categories.first().equals("tacodan.tacolib")

    }

}
