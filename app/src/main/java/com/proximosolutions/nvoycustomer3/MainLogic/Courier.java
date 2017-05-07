package com.proximosolutions.nvoycustomer3.MainLogic;

/**
 * Created by Isuru Tharanga on 3/25/2017.
 */

public class Courier extends NvoyUser {

    /*private ArrayList<Parcel> pastDeliveries;
    private ArrayList<Parcel> inTransit;*/
    private Location currentLocation;
    private boolean expressCourier;


        /*public ArrayList<Parcel> getPastDeliveries() {
            return pastDeliveries;
        }

        public void setPastDeliveries(ArrayList<Parcel> pastDeliveries) {
            this.pastDeliveries = pastDeliveries;
        }

        public ArrayList<Parcel> getInTransit() {
            return inTransit;
        }

        public void setInTransit(ArrayList<Parcel> inTransit) {
            this.inTransit = inTransit;
        }

        public Location getCurrentLocation() {
            return currentLocation;
        }

        public void setCurrentLocation(Location currentLocation) {
            this.currentLocation = currentLocation;
        }*/

    public void calculateFair(){}
    public void notifyDelivery(){}


    public boolean isExpressCourier() {
        return expressCourier;
    }

    public void setExpressCourier(boolean expressCourier) {
        this.expressCourier = expressCourier;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }
}
