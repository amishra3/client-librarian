package com.citytechinc.cq.clientlibs.services.clientlibs.actor.messages;

public class ClientLibraryStatisticsRequestMessage {

    private static ClientLibraryStatisticsRequestMessage instance = new ClientLibraryStatisticsRequestMessage();

    private ClientLibraryStatisticsRequestMessage() {

    }

    public static ClientLibraryStatisticsRequestMessage instance() {
        return instance;
    }

}
