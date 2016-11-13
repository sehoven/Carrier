package comcmput301f16t01.github.carrier;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import android.location.Location;

/**
 * Represents a request for a ride.
 */
public class Request {
    static final int OPEN = 1;            // A user has made the request but no drivers have accepted.
    static final int OFFERED = 2;         // One or more drivers have offered to fulfill the request.
    static final int CONFIRMED = 3;       // The user has chosen a driver and accepted one request.
    static final int COMPLETE = 4;        // The user has gotten to their destination (and payed?)
    static final int PAID = 7;
    static final int CANCELLED = 9;        // The rider has cancelled their request

    /** The current status of a this request */
    private int status = OPEN;

    /** The user who made the request. */
    private User rider;

    /**
     * The driver that the user has chosen to drive for the request
     */
    private User chosenDriver = null;
    /** A list of drivers who have offered to complete the request (but have not been accepted) */
    private ArrayList<User> offeringDrivers;

    /** The "from" of the request, where the user wants to go from */
    private Location start;

    /** The "end" of the request, where the user want to go */
    private Location end;

    /** A description provided by the rider */
    private String description;

    /** The price the requesting user is willing to pay for the request to be complete */
    private int fare;

    /** When elastic searching, can search if this is true to notify the rider about the request */
    private boolean needToNotifyRider = false;

    /** When elastic searching, can search if this is true to notify the driver about the request */
    private boolean needToNotifyDriver = false;

    /** For use with Elastic Search, is the unique ID given to it */
    private String elasticID;


    //TODO maybe add the location strings to description by default? Just in case keywords are locations.
    // Constructor with description
    public Request(@NonNull User requestingRider, @NonNull Location requestedStart,
                   @NonNull Location requestedEnd, String description) {
        this.rider = requestingRider;
        this.start = requestedStart;
        this.end = requestedEnd;
        this.description = description;
        this.offeringDrivers = new ArrayList<User>();
    }

    // Constructor without description TODO do we need this?
    public Request(User rider, Location start, Location end) {
        this.rider = rider;
        this.start = start;
        this.end = end;
        this.offeringDrivers = new ArrayList<User>();
        this.description = "";

    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int newStatus) {
        // Does not allow the canceling of completed requests or paid requests
        if ((this.status == COMPLETE) && (newStatus == CANCELLED))
            return; // Do nothing
        if ((this.status == PAID) && (newStatus == CANCELLED)) {
            return; // Do nothing
        }
        this.status = newStatus;
        // TODO make sure you do this right - Mandy (i.e. check that the status can change from one state to another)
        // TODO make an actual test for this (Mandy)
    }

    public void setFare(int fare) {
        this.fare = fare;
    }

    public void setChosenDriver(User driver) {
        this.chosenDriver = driver;
    }

    public User getChosenDriver() {
        return this.chosenDriver;
    }

    // possibly get rid of?
    public int getFareEstimate(Double distance, Double duration) {
        FareCalculator fareCalc = new FareCalculator();
        return fareCalc.getEstimate(distance, duration);
    }

    public ArrayList<User> getOffers() {
        return new ArrayList<User>();
    }

    public User getRider() {
        return this.rider;
    }

    public Location getStart() {
        return this.start;
    }

    public Location getEnd() {
        return this.end;
    }

    public User getConfirmedDriver() {
        return chosenDriver;
    }

    public ArrayList<User> getOfferedDrivers() {
        return this.offeringDrivers;
    }

    public String getDescription() {
        return description;
    }

    public int getFare() {
        return fare;
    }

    public void setId(String id) {
        this.elasticID = id;
    }

    public String getId() {
        return elasticID;
    }

    @Override
    public String toString() {
        String requestAsString = "Request From: " + rider.getUsername() + "\n";
        requestAsString += "Description: " + description;
        return requestAsString;
    }

    /**
     * Will addthe driver to the list of offering drivers.
     * @param offeredDriver The driver that is making the offer.
     */
    public void addOfferingDriver(User offeredDriver) {
        offeringDrivers.add(offeredDriver);
        if (status == Request.OPEN) {
            this.setStatus(Request.OFFERED);
        }
    }

    public void confirmDriver(User confirmedDriver) {
        chosenDriver = confirmedDriver;
        setStatus(Request.CONFIRMED);
    }

}