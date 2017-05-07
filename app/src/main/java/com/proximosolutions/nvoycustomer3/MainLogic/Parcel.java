package com.proximosolutions.nvoycustomer3.MainLogic;

/**
 * Created by Isuru Tharanga on 3/25/2017.
 */

public class Parcel {
    private String parcelID;
    private String itemDescription;
    private NvoyUser sender;
    private NvoyUser receiver;
    private NvoyUser carrier;
    private double deliveryFair;
    private Location currentLocation;
    private Status status;
}
