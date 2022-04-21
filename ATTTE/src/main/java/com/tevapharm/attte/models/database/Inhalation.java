package com.tevapharm.attte.models.database;

public class Inhalation {

    public Event event = new Event();
    public Device device = new Device();
    public GeoLocation geoLocation = new GeoLocation();

    public class Event {
        public int id;
        public String time;
        public String category;
        public int startOffset;
        public int duration;
        public int peakFlow;
        public int peakOffset;
        public double volume;
        public int status;
    }

    public class Device {
        public Long serialNumber;
    }

    public class GeoLocation {
        public double longitude;
        public double latitude;
    }
}
