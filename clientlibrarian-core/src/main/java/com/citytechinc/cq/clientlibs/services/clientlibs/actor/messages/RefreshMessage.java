package com.citytechinc.cq.clientlibs.services.clientlibs.actor.messages;

public class RefreshMessage {

    private static final RefreshMessage instance = new RefreshMessage();

    private RefreshMessage() {

    }

    public static RefreshMessage getInstance() {
        return instance;
    }

}
