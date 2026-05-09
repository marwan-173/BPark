package common;

public enum ClientRequest {
    RESERVE_PARKING_AUTO,
    CHECK_SUBSCRIBER,
    DROP_OFF_CAR,
    PICK_UP_CAR,
    DisplayParkings,
    DisplayReservations,
    UPDATE_DETAILS,
    ATTENDANT_LOGIN,
    REGISTER_SUBSCRIBER,
    DisplaySubscribers,
    AttendantParking,
    MANAGER_LOGIN,
    GET_FREE_SPOTS,
    GET_PARKING_REPORT,
    GET_SUBSCRIBER_REPORT,
    GET_CLIENT_PROFILE,
	SEND_CODE_TO_EMAIL,
	CancelReservation,
    DISCONNECT;
	

    public static ClientRequest fromString(String value) {
        for (ClientRequest req : values()) {
            if (req.name().equalsIgnoreCase(value)) {
                return req;
            }
        }
        throw new IllegalArgumentException("Unknown request type: " + value);
    }
}