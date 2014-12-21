package com.db.train.atm;

import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

public class ATMData implements Serializable {
    private double longitude;
    private double latitude;
    private double bid;
    private double ask;
    private static final double MIN_BID = 50.0;
    private static final double MAX_BID = 80.0;
    private static final double MIN_ASK = 60.0;
    private static final double MAX_ASK = 100.0;

    public ATMData(double longitude, double latitude, double bid, double ask) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.bid = bid;
        this.ask = ask;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getBid() {
        return bid;
    }

    public double getAsk() {
        return ask;
    }

    public static ATMData generate() {
        double randomBid = MIN_BID + (MAX_BID - MIN_BID) * ThreadLocalRandom.current().nextDouble(1);
        double randomAsk = MIN_ASK + (MAX_ASK - MIN_ASK) * ThreadLocalRandom.current().nextDouble(1);
        return new ATMData(ThreadLocalRandom.current().nextDouble(),
                ThreadLocalRandom.current().nextDouble(), randomBid, randomAsk);
    }

    @Override
    public String toString() {
        return "ATMData{" +
                "longitude=" + longitude +
                ", latitude=" + latitude +
                ", bid=" + bid +
                ", ask=" + ask +
                '}';
    }
}
