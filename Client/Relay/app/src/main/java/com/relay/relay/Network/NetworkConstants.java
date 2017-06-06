package com.relay.relay.Network;

/**
 * Created by omer on 05/06/2017.
 */

public interface NetworkConstants {


    int SECOND = 1000;
    int MINUTE = 1000*60;

    // Interval time to sync with server
    int INTERVAL_SYNC_WITH_SERVER =  1*MINUTE;

    // api
    String API_SYNC_METADATA = "sync/metadata/";
    String API_SYNC_BODY = "sync/body/";

    // steps in sync
    int STEP_1_META_DATA = 115;
    int STEP_2_BODY = 116;

    // sync status
    int SYNC_WITH_SERVER = 112;
    int NOT_SYNC_WITH_SERVER = 113;

    // Server url
    String RELAY_URL = "https://relayproject-staging.herokuapp.com/api/";

    // Get node
    String NODE = "node/";

    // sync update to manager
    int NEW_RELAY_MESSAGE = 120;
    int START_SYNC = 121;
    int FINISH_SYNC = 122;
    int ERROR_WHILE_SYNC = 123;
    int RESPONSE = 124;

    // Delimiter in bluetoothConnected
    String DELIMITER = "<!-12341234@12341234-!>";

}
