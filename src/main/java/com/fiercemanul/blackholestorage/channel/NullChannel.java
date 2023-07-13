package com.fiercemanul.blackholestorage.channel;

public class NullChannel extends ServerChannel {

    public NullChannel() {
        super();
    }

    @Override
    public boolean isRemoved() {
        return true;
    }
}