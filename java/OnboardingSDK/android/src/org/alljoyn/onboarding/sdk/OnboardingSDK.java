/******************************************************************************
 * Copyright (c) 2014, AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 ******************************************************************************/

package org.alljoyn.onboarding.sdk;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import org.alljoyn.about.AboutKeys;
import org.alljoyn.about.AboutService;
import org.alljoyn.about.transport.AboutTransport;
import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.Variant;
import org.alljoyn.onboarding.OnboardingService.AuthType;
import org.alljoyn.onboarding.client.OnboardingClient;
import org.alljoyn.onboarding.client.OnboardingClientImpl;
import org.alljoyn.onboarding.sdk.DeviceResponse;
import org.alljoyn.onboarding.sdk.DeviceResponse.ResponseCode;
import org.alljoyn.onboarding.transport.OnboardingTransport;
import org.alljoyn.services.common.AnnouncementHandler;
import org.alljoyn.services.common.BusObjectDescription;
import org.alljoyn.services.common.ClientBase;
import org.alljoyn.services.common.ServiceAvailabilityListener;
import org.alljoyn.services.common.utils.TransportUtil;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

/**
 * OnboardingSDK The OnboardingSDK class streamlines the process of onboarding
 * for Android application developers. The SDK encapsulates the Wi-Fi and
 * AllJoyn sessions that are part of the process. The Onboarding SDK API
 * provides: 1. Discovery of potential target networks, by calling
 * {@link #scanWiFi()} 2. Discovery of potential onboardees. 3. A single call,
 * {@link #runOnboarding(OnboardingConfiguration)}, for running the onboarding
 * flow. Intents for error and progress reporting.
 */
public class OnboardingSDK {

    /**
     * Activity Action:WIFI has been connected by the API
     */
    static final String WIFI_CONNECTED_BY_REQUEST_ACTION = "org.alljoyn.onboardingsdk.wifi.connection_by_request";

    /**
     * Activity Action:WIFI connection has timed out
     */
    static final String WIFI_TIMEOUT_ACTION = "org.alljoyn.onboardingsdk.wifi.time_out";

    /**
     * Activity Action:WIFI authentication has occurred
     */
    static final String WIFI_AUTHENTICATION_ERROR = "org.alljoyn.onboardingsdk.wifi.authentication_error";

    /**
     * The lookup key for WifiConfiguration details after connection request.
     */
    static final String EXTRA_WIFI_WIFICONFIGURATION = "org.alljoyn.intent_keys.WifiConfiguration";

    /**
     * The lookup key for onboardee list of access points
     */
    public static final String EXTRA_ONBOARDEES = "org.alljoyn.onboardingsdk.intent_keys.ONBOARDEES";

    /**
     * The lookup key for target list of access points
     */
    public static final String EXTRA_TARGETS = "org.alljoyn.onboardingsdk.intent_keys.TARGETS";

    /**
     * The lookup key for NewState reported by the SDK
     */
    public static final String EXTRA_NEW_STATE = "org.alljoyn.onboardingsdk.intent_keys.newstate";

    /**
     * The lookup key for WIFI details reported by the SDK
     */
    public static final String EXTRA_WIFI_NETWORK = "org.alljoyn.onboardingsdk.intent_keys.wifinet";

    /**
     * The lookup key for ERROR details reported by the SDK
     */
    public static final String EXTRA_ERROR_DETAILS = "org.alljoyn.onboardingsdk.intent_keys.error";
    
    /**
     *   The lookup key for DEVICE_INFORMATION  reported by the SDK
     */
    public static final String EXTRA_DEVICE_INFORMATION = "org.alljoyn.onboardingsdk.intent_keys.deviceInfo";
    
    /**
     * Activity Action: indicating that the WIFI scan has been completed
     */
    public static final String WIFI_SCAN_RESULTS_AVAILABLE_ACTION = "org.alljoyn.onboardingsdk.scan_result_available";

    /**
     * Activity Action: indicating state changes of the SDK
     */
    public static final String STATE_CHANGE_ACTION = "org.alljoyn.onboardingsdk.state_change";

    /**
     * Activity Action: indication error encountered by the SDK
     */
    public static final String ERROR = "org.alljoyn.onboardingsdk.error";

    public static enum ErrorState {
	/**
	 * WIFI authentication error
	 */
	WIFI_AUTH(0),
	/**
	 * WIFI connection request timed out
	 */
	WIFI_TIMEOUT(1),
	/**
	 * Announcement failed to arrive in due time from onboardee while
	 * connected to onboardee WIFI
	 */
	FIND_ONBOARDEE_TIMEOUT(2),
	/**
	 * Failed to establish connection
	 */
	CONNECTION_FAILED(3),
	/**
	 * Failed to send WIFI credentials to oboardee
	 */
	CONFIGURATION_FAILED(4),
	/**
	 * Announcement failed to arrive in due time from onboardee while
	 * connected to target WIFI
	 */
	VERIFICATION_TIMEOUT(5),
	/**
	 * Device doesn't support onboarding interface
	 */
	ONBOARDING_NOT_SUPPORTED(6),	
	/**
	 * Failed to offboard the device
	 */
	OFFBOARDING_FAILED(7),	
	/**
	 * SDK has encountered interanl error
	 */
	INTERNAL_ERROR(8);
	
	
	private int value;

	private static String strings[] = { "WIFI_AUTH", "WIFI_TIMEOUT", "FIND_ONBOARDEE_TIMEOUT", "CONNECTION_FAILED", "CONFIGURATION_FAILED", "VERIFICATION_TIMEOUT", "ONBOARDING_NOT_SUPPORTED",
		"OFFBOARDING_FAILED","INTERNAL_ERROR" };

	private ErrorState(int value) {
	    this.value = value;
	}

	public int getValue() {
	    return value;
	}

	public static ErrorState getErrorStateByValue(int value) {
	    ErrorState retType = null;
	    for (ErrorState type : ErrorState.values()) {
		if (value == type.getValue()) {
		    retType = type;
		    break;
		}
	    }
	    return retType;
	}

	public static ErrorState getErrorStateByString(String str) {
	    ErrorState retType = null;
	    for (int i = 0; i < strings.length; i++) {
		if (str.equals(strings[i])) {
		    return ErrorState.values()[i];
		}
	    }
	    return retType;
	}

	@Override
	public String toString() {
	    return strings[value];
	}

    };

    public static enum WifiNetState {
	/**
	 * onboardee device
	 */
	ONBOARDEE(0),
	/**
	 * target device
	 */
	TARGET(1),
	/**
	 * original device
	 */
	ORIGINAL(2),
	/**
	 * other
	 */
	OTHER(3);

	private int value;

	private static String strings[] = { "ONBOARDEE", "TARGET", "ORIGINAL", "OTHER", };

	private WifiNetState(int value) {
	    this.value = value;
	}

	public int getValue() {
	    return value;
	}

	public static WifiNetState getWifiNetStateByValue(int value) {
	    WifiNetState retType = null;
	    for (WifiNetState type : WifiNetState.values()) {
		if (value == type.getValue()) {
		    retType = type;
		    break;
		}
	    }
	    return retType;
	}

	public static WifiNetState getWifiNetStateByString(String str) {
	    WifiNetState retType = null;
	    for (int i = 0; i < strings.length; i++) {
		if (str.equals(strings[i])) {
		    return WifiNetState.values()[i];
		}
	    }
	    return retType;
	}

	@Override
	public String toString() {
	    return strings[value];
	}

    };

    public static enum NewState {
	/**
	 * connecting to onboardee Wi-Fi
	 */
	CONNECTING_WIFI(0),
	/**
	 * connected to onboardee Wi-Fi
	 */
	CONNECTED_WIFI(1),
	/**
	 * waiting for announcement from onboardee
	 */
	FINDING_ONBOARDEE(2),
	/**
	 * announcement received from onboardee
	 */
	FOUND_ONBOARDEE(3),
	/**
	 * creating AllJoyn session with onboardee
	 */
	CONNECTING_ONBOARDEE(4),
	/**
	 * AllJoyn session established with onboardee
	 */
	CONNECTED_ONBOARDEE(5),
	/**
	 * Sending target credentials to onboardee
	 */
	CONFIGURING_ONBOARDEE(6),
	/**
	 * onboardee received target credentials
	 */
	CONFIGURED_ONBOARDEE(7),
	/**
	 * connecting to WIFI target
	 */
	CONNECTING_TARGET(8),
	/**
	 * WIFI connection with target established
	 */
	CONNECTED_TARGET(9),
	/**
	 * wait for announcement from onboardee over target WIFI
	 */
	VERIFYING_ONBOARDED(10),
	/**
	 * announcement from onboardee over target WIFI has been received
	 */
	VERIFIED_ONBOARDED(11),
	/**
	 * aborting
	 */
	ABORTING(12);

	private int value;

	private static String strings[] = { "CONNECTING_WIFI", "CONNECTED_WIFI", "FINDING_ONBOARDEE", "FOUND_ONBOARDEE", "CONNECTING_ONBOARDEE", "CONNECTED_ONBOARDEE", "CONFIGURING_ONBOARDEE",
		"CONFIGURED_ONBOARDEE", "CONNECTING_TARGET", "CONNECTED_TARGET", "VERIFYING_ONBOARDED", "VERIFIED_ONBOARDED", "ABORTING" };

	private NewState(int value) {
	    this.value = value;
	}

	public int getValue() {
	    return value;
	}

	public static NewState getNewStateByValue(int value) {
	    NewState retType = null;
	    for (NewState type : NewState.values()) {
		if (value == type.getValue()) {
		    retType = type;
		    break;
		}
	    }
	    return retType;
	}

	public static NewState getNewStateByString(String str) {
	    NewState retType = null;
	    for (int i = 0; i < strings.length; i++) {
		if (str.equals(strings[i])) {
		    return NewState.values()[i];
		}
	    }
	    return retType;
	}

	@Override
	public String toString() {
	    return strings[value];
	}
    }

    /**
     * TAG for debug information
     */
    private final static String TAG = "OnBoardingSDK";

    /**
     * Default timeout for Wi-Fi connection
     */
    public static final int DEFAULT_WIFI_CONNECTION_TIMEOUT = 20000;

    /**
     * Default timeout for waiting for
     * {@link AboutTransport#Announce(short, short, org.alljoyn.services.common.BusObjectDescription[], java.util.Map)}
     */
    public static final int DEFAULT_ANNOUNCEMENT_TIMEOUT = 25000;

    /**
     * OnboardingSDK singelton
     */
    private static OnboardingSDK onboardingSDK = null;

    /**
     * Application context
     */
    private Context context = null;

    /**
     * HandlerThread for the state machine looper
     */
    private static HandlerThread stateHandlerThread = new HandlerThread("OnboardingSDKLooper");
    /**
     * Handler for OnboardingSDK state changing messages.
     */
    private static Handler stateHandler = null;

    /**
     * Stores the AllJoynManagmentCallback object
     */
    private AllJoynManagmentCallback alljoynManagmentCallback = null;

    /**
     * Stores the OnboardingsdkWifiManager object
     */
    private OnboardingSDKWifiManager onboardingSDKWifiManager = null;

    /**
     * Stores the OnboardingConfiguration object
     */
    private OnboardingConfiguration onboardingConfiguration = null;

    /**
     * IntentFilter used to filter out intents of WIFI messages received from
     * OnboardingsdkWifiManager
     */
    private IntentFilter wifiIntentFilter = new IntentFilter();

    /**
     * BroadcastReceiver for intents from OnboardingsdkWifiManager while running
     * the onboarding process.
     */
    private BroadcastReceiver onboardingWifiBroadcastReceiver = null;

    /**
     * BroadcastReceiver for intents from OnboardingsdkWifiManager while running
     * {@link #connectToNetwork(WiFiNetworkConfiguration, int)}.
     */
    private BroadcastReceiver connectToNetworkWifiBroadcastReceiver = null;

    /**
     * Stores the OnboardingClient object used to communicate with
     * OnboardingService.
     */
    private OnboardingClient onboardingClient = null;

    /**
     * Stores the BusAttachment needed for accessing Alljoyn framework.
     */
    private BusAttachment bus = null;

    /**
     * Timer used for managing announcement timeout
     */
    private Timer announcementTimeout = new Timer();

    /**
     * Stores the OnboardingSDK state machine state.
     */
    private State currentState = State.IDLE;

    /**
     * Stores information needed by the onboarding process.
     */
    private DeviceData deviceData = null;

    /**
     * Stores data related to onboarding process.
     */
    private static class DeviceData {

	public AnnounceData getAnnounceObject() {
	    return announceData;
	}

	public void setAnnounceData(AnnounceData announceData) throws BusException {
	    this.announceData = announceData;
	    Map<String, Object> newMap = TransportUtil.fromVariantMap(announceData.getServiceMetadata());
	    appUUID = (UUID) newMap.get(AboutKeys.ABOUT_APP_ID);
	}

	private AnnounceData announceData = null;

	public UUID getAppUUID() {
	    return appUUID;
	}

	private UUID appUUID = null;

    }

    /**
     * An internal class to store the Announcement received by the AboutService.
     */
    private static class AnnounceData {

	public String getServiceName() {
	    return serviceName;
	}

	public BusObjectDescription[] getObjectDescriptions() {
	    return objectDescriptions;
	}

	public Map<String, Variant> getServiceMetadata() {
	    return serviceMetadata;
	}

	public short getPort() {
	    return port;
	}

	private String serviceName;
	private short port;
	private BusObjectDescription[] objectDescriptions;
	private Map<String, Variant> serviceMetadata;

	public AnnounceData(String serviceName, short port, BusObjectDescription[] objectDescriptions, Map<String, Variant> serviceMetadata) {
	    this.serviceName = serviceName;
	    this.port = port;
	    this.objectDescriptions = objectDescriptions;
	    this.serviceMetadata = serviceMetadata;
	}
    }

    /**
     * An enumeration of the onboaring state machine.
     */
    private static enum State {
	/**
	 * start state
	 */
	IDLE(0),
	/**
	 * connecting to onboardee device Wi-Fi
	 */
	CONNECTING_TO_ONBOARDEE(10),
	/**
	 * waiting for announcement on onboardee Wi-Fi
	 */
	WAITING_FOR_ONBOARDEE_ANNOUNCEMENT(11),
	/**
	 * announcement received on onboardee Wi-Fi
	 */
	ONBOARDEE_ANNOUNCEMENT_RECEIVED(12),
	/**
	 * configuring onboardee with target credentials
	 */
	CONFIGURING_ONBOARDEE(13),
	/**
	 * connecting to target Wi-Fi AP
	 */
	CONNECTING_TO_TARGET_WIFI_AP(20),
	/**
	 * waiting for announcement on target  Wi-Fi from onboardee
	 */
	WAITING_FOR_TARGET_ANNOUNCE(21),
	/**
	 * announcement received on target  Wi-Fi from onboardee
	 */
	TARGET_ANNOUNCEMENT_RECEIVED(22),
	/**
	 * error connecting to onboardee device Wi-Fi
	 */	
	ERROR_CONNECTING_TO_ONBOARDEE(110),
	/**
	 * error waiting for announcement on onboardee Wi-Fi
	 */
	ERROR_WAITING_FOR_ONBOARDEE_ANNOUNCEMENT(111),
	/**
	 * error announcement received on onboardee Wi-Fi
	 */
	ERROR_ONBOARDEE_ANNOUNCEMENT_RECEIVED(112),
	/**
	 * error configuring onboardee with target credentials
	 */
	ERROR_CONFIGURING_ONBOARDEE(113),
	/**
	 * error connecting to target  Wi-Fi AP
	 */
	ERROR_CONNECTING_TO_TARGET_WIFI_AP(120),
	/**
	 * error waiting for announcement on target  Wi-Fi  from onboardee
	 */
	ERROR_WAITING_FOR_TARGET_ANNOUNCE(121);
	/**
	 * error announcement received on target  Wi-Fi  from onboardee
	 */
	
	

	private int value;

	private State(int value) {
	    this.value = value;
	}

	public int getValue() {
	    return value;
	}

	public static State getStateByValue(int value) {
	    State retType = null;
	    for (State type : State.values()) {
		if (value == type.getValue()) {
		    retType = type;
		    break;
		}
	    }
	    return retType;
	}
    }

    /**
     * @return instance of the OnBoardingSDK
     */
    public static OnboardingSDK getInstance() {
	if (onboardingSDK == null) {
	    onboardingSDK = new OnboardingSDK();
	}
	return onboardingSDK;

    }

    /**
     * constructor of OnboardingSDK
     */
    private OnboardingSDK() {

	wifiIntentFilter.addAction(WIFI_CONNECTED_BY_REQUEST_ACTION);
	wifiIntentFilter.addAction(WIFI_TIMEOUT_ACTION);
	wifiIntentFilter.addAction(WIFI_AUTHENTICATION_ERROR);

	stateHandlerThread.start();
	stateHandler = new Handler(stateHandlerThread.getLooper()) {
	    @Override
	    public void handleMessage(Message msg) {
		onHandleCommandMessage(msg);
	    }
	};
	
    }

    /**
     * Initializes the SDK singleton with the current application configuration.
     * Registers AnnouncementHandler to receive Announcements
     * 
     * @param context
     *            The application context
     * @param allJoynManagmentCallback
     *            a callback for invoking BusAttachment recycling.
     * @throws IllegalArgumentException
     *             if either of the parameters is null.
     * @throws IllegalStateException
     *             if already initialized.
     */
    public void init(Context context, AllJoynManagmentCallback allJoynManagmentCallback) throws IllegalArgumentException, IllegalStateException {
	if (context == null || allJoynManagmentCallback == null) {
	    throw new IllegalArgumentException();
	}
	if (this.context != null || this.onboardingSDKWifiManager != null) {
	    throw new IllegalStateException();
	}
	this.context = context;
	this.onboardingSDKWifiManager = new OnboardingSDKWifiManager(context);
	this.alljoynManagmentCallback = allJoynManagmentCallback;

    }

    /**
     * Updates the SDK singleton with the current application configuration.
     * Registers AnnouncementHandler to receive Announcements
     * 
     * @param aboutService
     * @param bus
     *            The AllJoyn BusAttachment
     * @throws IllegalArgumentException
     *             if any of the parameters is null
     */
    public void update(AboutService aboutService, BusAttachment bus) throws IllegalArgumentException {

	if (aboutService == null || bus == null) {
	    throw new IllegalArgumentException();
	}

	this.bus = bus;
	aboutService.addAnnouncementHandler(new AnnouncementHandler() {

	    @Override
	    public void onAnnouncement(final String serviceName, final short port, final BusObjectDescription[] objectDescriptions, final Map<String, Variant> serviceMetadata) {

		Log.d(TAG, "onAnnouncement: received ");
		Map<String, Object> newMap = null;
		try {
		    newMap = TransportUtil.fromVariantMap(serviceMetadata);
		} catch (BusException e) {
		    e.printStackTrace();
		}
		if (newMap == null)
		    return;
		UUID uniqueId = (UUID) newMap.get(AboutKeys.ABOUT_APP_ID);

		if (uniqueId == null) {
		    Log.e(TAG, "onAnnouncement: received null device uuid!! ignoring.");
		    return;
		} else {
		    Log.e(TAG, "onAnnouncement: received UUID " + uniqueId);
		}

		switch (currentState) {

		case WAITING_FOR_ONBOARDEE_ANNOUNCEMENT: {
		    setState(State.ONBOARDEE_ANNOUNCEMENT_RECEIVED, new AnnounceData(serviceName, port, objectDescriptions, serviceMetadata));
		}
		    break;
		case WAITING_FOR_TARGET_ANNOUNCE: {
		    if (deviceData != null && deviceData.getAnnounceObject() != null && deviceData.getAppUUID() != null) {
			Log.e(TAG, "onAnnouncement: device  UUID " + deviceData.getAppUUID());
			if (deviceData.getAppUUID().compareTo(uniqueId) == 0) {
			    setState(State.TARGET_ANNOUNCEMENT_RECEIVED, new AnnounceData(serviceName, port, objectDescriptions, serviceMetadata));
			}

		    }
		}
		    break;
		default:
		    break;
		}
	    }// onAnnouncement

	    /**
	     * Irrelevant to the process of onboarding
	     */
	    @Override
	    public void onDeviceLost(String deviceName) {
		Log.d(TAG, "Received onDeviceLost for busName " + deviceName);
	    }
	});
    }

    /**
     * Handles the CONNECT_TO_ONBOARDEE state. Listen to WIFI intents from
     * OnboardingsdkWifiManager Requests from OnboardingsdkWifiManager to
     * connect to the Onboardee. if successful moves to the next state otherwise
     * send error intent and returns to IDLE state.
     */
    private void handleConnectToOnboardeeState() {
	final Bundle extras = new Bundle();
	onboardingWifiBroadcastReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		Log.d(TAG, "handleConnectToOnboardeeState onReceive action=" + action);
		if (action == null) {
		    return;
		}

		if (WIFI_CONNECTED_BY_REQUEST_ACTION.equals(action)) {
		    context.unregisterReceiver(onboardingWifiBroadcastReceiver);
		    extras.clear();
		    extras.putString(EXTRA_NEW_STATE, NewState.CONNECTED_WIFI.toString());
		    extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.ONBOARDEE.toString());
		    sendBroadcast(STATE_CHANGE_ACTION, extras);
		    setState(State.WAITING_FOR_ONBOARDEE_ANNOUNCEMENT);
		}		
		if (WIFI_TIMEOUT_ACTION.equals(action)) {
		    context.unregisterReceiver(onboardingWifiBroadcastReceiver);
		    extras.clear();
		    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.WIFI_TIMEOUT.toString());
		    extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.ONBOARDEE.toString());
		    sendBroadcast(ERROR, extras);
		    setState(State.ERROR_CONNECTING_TO_ONBOARDEE);
		}
		if (WIFI_AUTHENTICATION_ERROR.equals(action)) {
		    extras.clear();
		    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.WIFI_AUTH.toString());
		    extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.ONBOARDEE.toString());
		    sendBroadcast(ERROR, extras);
		    context.unregisterReceiver(onboardingWifiBroadcastReceiver);
		    setState(State.ERROR_CONNECTING_TO_ONBOARDEE);

		}
	    }
	};
	context.registerReceiver(onboardingWifiBroadcastReceiver, wifiIntentFilter);
	extras.clear();
	extras.putString(EXTRA_NEW_STATE, NewState.CONNECTING_WIFI.toString());
	extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.ONBOARDEE.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);
	onboardingSDKWifiManager.connectToWifiAP(onboardingConfiguration.getOnboardee().getSSID(), onboardingConfiguration.getOnboardee().getAuthType(), onboardingConfiguration.getOnboardee()
		.getPassword(), onboardingConfiguration.getOnboardeeConnectionTimeout());

    }

    /**
     * handleWaitForOnboardeeAnnounceState handles the
     * WAIT_FOR_ONBOARDEE_ANNOUNCE state. set a timer with using
     * startAnnouncementTimeout. waits for an Announcement which should arrive
     * from the onAnnouncement handler.
     */
    private void handleWaitForOnboardeeAnnounceState() {
	Bundle extras = new Bundle();
	alljoynManagmentCallback.reconnectToBus();
	extras.clear();
	extras.putString(EXTRA_NEW_STATE, NewState.FINDING_ONBOARDEE.toString());
	extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.ONBOARDEE.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);
	if (!startAnnouncementTimeout()) {
	    extras.clear();
	    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.INTERNAL_ERROR.toString());
	    sendBroadcast(ERROR, extras);
	    setState(State.ERROR_WAITING_FOR_ONBOARDEE_ANNOUNCEMENT);
	}
    }

    /**
     * handleOnboardeeAnnouncementReceivedState handles the
     * ONBOARDEE_ANNOUNCEMENT_RECEIVED state. stops the Announcement time out
     * timer and check that the board supports the 'org.alljoyn.Onboarding'
     * interface if so move to next state else return to IDLE state.
     * 
     * @param announceData2
     *            contains the information of the Announcement .
     */

    private void handleOnboardeeAnnouncementReceivedState(AnnounceData announceData) {

	stopAnnouncementTimeout();

	Bundle extras = new Bundle();
	extras.clear();
	extras.putString(EXTRA_NEW_STATE, NewState.FOUND_ONBOARDEE.toString());
	extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.ONBOARDEE.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);

	if (isSeviceSupported(announceData.getObjectDescriptions(), OnboardingTransport.INTERFACE_NAME)) {
	    Log.d(TAG, "handleOnboardeeAnnouncementReceivedState ONBOARDEE_ANNOUNCEMENT_RECEIVED supporting  org.alljoyn.Onboarding");
	    deviceData = new DeviceData();
	    try {
		deviceData.setAnnounceData(announceData);
		setState(State.CONFIGURING_ONBOARDEE, announceData);
	    } catch (BusException e) {
		Log.e(TAG, "handleOnboardeeAnnouncementReceivedState DeviceData.setAnnounceObject failed with BusException. ", e);
		extras.clear();
		extras.putString(EXTRA_ERROR_DETAILS, ErrorState.CONFIGURATION_FAILED.toString());
		sendBroadcast(ERROR, extras);
		setState(State.ERROR_ONBOARDEE_ANNOUNCEMENT_RECEIVED);
	    }
	} else {
	    extras.clear();
	    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.ONBOARDING_NOT_SUPPORTED.toString());
	    sendBroadcast(ERROR, extras);
	    setState(State.ERROR_ONBOARDEE_ANNOUNCEMENT_RECEIVED);
	}
    }

    /**
     * handleConfigureOnboardeeState handles the CONFIGURE_ONBOARDEE state.
     * calls onboardDevice to send target information to the board. in case
     * successful moves to next step else return to IDLE state.
     * 
     * @param announceData
     *            contains the information of the Announcement
     */
    private void handleConfigureOnboardeeState(AnnounceData announceData) {

	Bundle extras = new Bundle();
	extras.clear();
	extras.putString(EXTRA_NEW_STATE, NewState.CONFIGURING_ONBOARDEE.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);

	if (onboardDevice(announceData).getStatus() == ResponseCode.Status_OK) {
	    extras.clear();
	    extras.putString(EXTRA_NEW_STATE, NewState.CONFIGURED_ONBOARDEE.toString());
	    sendBroadcast(STATE_CHANGE_ACTION, extras);
	    setState(State.CONNECTING_TO_TARGET_WIFI_AP);
	} else {
	    extras.clear();
	    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.CONFIGURATION_FAILED.toString());
	    sendBroadcast(ERROR, extras);
	    setState(State.ERROR_CONFIGURING_ONBOARDEE);
	}
    }

    /**
     * handleConnectToTargetState handles the CONNECT_TO_TARGET state. Listen to
     * WIFI intents from OnboardingsdkWifiManager Requests from
     * OnboardingsdkWifiManager to connect to the Target. if successful moves to
     * the next state otherwise send error intent and returns to IDLE state.
     */
    private void handleConnectToTargetState() {
	final Bundle extras = new Bundle();
	onboardingWifiBroadcastReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();
		Log.d(TAG, "onReceive action=" + action);
		if (action != null) {

		    if (WIFI_CONNECTED_BY_REQUEST_ACTION.equals(action)) {
			context.unregisterReceiver(onboardingWifiBroadcastReceiver);
			extras.clear();
			extras.putString(EXTRA_NEW_STATE, NewState.CONNECTED_WIFI.toString());
			extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.TARGET.toString());
			sendBroadcast(STATE_CHANGE_ACTION, extras);
			extras.clear();
			extras.putString(EXTRA_NEW_STATE, NewState.CONNECTED_TARGET.toString());
			sendBroadcast(STATE_CHANGE_ACTION, extras);
			setState(State.WAITING_FOR_TARGET_ANNOUNCE);
		    }
		   
		    if (WIFI_TIMEOUT_ACTION.equals(action)) {
			context.unregisterReceiver(onboardingWifiBroadcastReceiver);
			extras.clear();
			extras.putString(EXTRA_ERROR_DETAILS, ErrorState.WIFI_TIMEOUT.toString());
			extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.TARGET.toString());
			sendBroadcast(ERROR, extras);
			setState(State.ERROR_CONNECTING_TO_TARGET_WIFI_AP);
		    }
		    if (WIFI_AUTHENTICATION_ERROR.equals(action)) {
			context.unregisterReceiver(onboardingWifiBroadcastReceiver);
			extras.clear();
			extras.putString(EXTRA_ERROR_DETAILS, ErrorState.WIFI_AUTH.toString());
			extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.TARGET.toString());
			sendBroadcast(ERROR, extras);
			setState(State.ERROR_CONNECTING_TO_TARGET_WIFI_AP);
		    }
		}
	    }
	};// receiver
	context.registerReceiver(onboardingWifiBroadcastReceiver, wifiIntentFilter);
	extras.clear();
	extras.putString(EXTRA_NEW_STATE, NewState.CONNECTING_WIFI.toString());
	extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.TARGET.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);
	extras.clear();
	extras.putString(EXTRA_NEW_STATE, NewState.CONNECTING_TARGET.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);
	onboardingSDKWifiManager.connectToWifiAP(onboardingConfiguration.getTarget().getSSID(), onboardingConfiguration.getTarget().getAuthType(), onboardingConfiguration.getTarget().getPassword(),
		onboardingConfiguration.getTargetConnectionTimeout());
    }

    /**
     * handleWaitForTargetAnnounceState handles the WAIT_FOR_TARGET_ANNOUNCE
     * state. set a timer with using startAnnouncementTimeout. waits for an
     * Announcement which should arrive from the onAnnouncement handler.
     */
    private void handleWaitForTargetAnnounceState() {

	Bundle extras = new Bundle();
	extras.clear();
	extras.putString(EXTRA_NEW_STATE, NewState.VERIFYING_ONBOARDED.toString());
	extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.TARGET.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);
	if (!startAnnouncementTimeout()) {
	    extras.clear();
	    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.INTERNAL_ERROR.toString());
	    sendBroadcast(ERROR, extras);
	    setState(State.ERROR_WAITING_FOR_TARGET_ANNOUNCE);
	}
    }

    /**
     * handleTargetAnnouncementReceivedState handles the
     * TARGET_ANNOUNCEMENT_RECEIVED state. stops the Announcement time out timer
     * send success broadcast.
     * 
     * @param announceData
     *            contains the information of the Announcement .
     */
    private void handleTargetAnnouncementReceivedState(AnnounceData announceData) {

	stopAnnouncementTimeout();

	Bundle extras = new Bundle();
	extras.clear();
	extras.putString(EXTRA_NEW_STATE, NewState.VERIFIED_ONBOARDED.toString());
	extras.putString(EXTRA_WIFI_NETWORK, WifiNetState.TARGET.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);
    }

    /**
     * onHandleCommandMessage handles the state machine transition.
     * 
     * @param msg
     */
    private void onHandleCommandMessage(Message msg) {
	if (msg == null)
	    return;

	switch (State.getStateByValue(msg.what)) {
	case IDLE:
	    Log.d(TAG, "onHandleCommandMessage IDLE");
	    currentState = State.IDLE;
	    break;
	case CONNECTING_TO_ONBOARDEE:
	    Log.d(TAG, "onHandleCommandMessage CONNECT_TO_ONBOARDEE");
	    currentState = State.CONNECTING_TO_ONBOARDEE;
	    handleConnectToOnboardeeState();
	    break;
	case WAITING_FOR_ONBOARDEE_ANNOUNCEMENT:
	    Log.d(TAG, "onHandleCommandMessage WAIT_FOR_ONBOARDEE_ANNOUNCE");
	    currentState = State.WAITING_FOR_ONBOARDEE_ANNOUNCEMENT;
	    handleWaitForOnboardeeAnnounceState();
	    break;
	case ONBOARDEE_ANNOUNCEMENT_RECEIVED:
	    Log.d(TAG, "onHandleCommandMessage ONBOARDEE_ANNOUNCEMENT_RECEIVED");
	    currentState = State.ONBOARDEE_ANNOUNCEMENT_RECEIVED;
	    handleOnboardeeAnnouncementReceivedState((AnnounceData) msg.obj);
	    break;
	case CONFIGURING_ONBOARDEE:
	    Log.d(TAG, "onHandleCommandMessage CONFIGURE_ONBOARDEE");
	    currentState = State.CONFIGURING_ONBOARDEE;
	    handleConfigureOnboardeeState((AnnounceData) msg.obj);
	    break;
	case CONNECTING_TO_TARGET_WIFI_AP:
	    Log.d(TAG, "onHandleCommandMessage CONNECT_TO_TARGET");
	    currentState = State.CONNECTING_TO_TARGET_WIFI_AP;
	    handleConnectToTargetState();
	    break;
	case WAITING_FOR_TARGET_ANNOUNCE:
	    Log.d(TAG, "onHandleCommandMessage WAIT_FOR_ONBOARDEE_ANNOUNCE");
	    currentState = State.WAITING_FOR_TARGET_ANNOUNCE;
	    alljoynManagmentCallback.reconnectToBus();
	    handleWaitForTargetAnnounceState();
	    break;
	case TARGET_ANNOUNCEMENT_RECEIVED:
	    Log.d(TAG, "onHandleCommandMessage TARGET_ANNOUNCEMENT_RECEIVED");
	    currentState = State.TARGET_ANNOUNCEMENT_RECEIVED;
	    handleTargetAnnouncementReceivedState((AnnounceData) msg.obj);
	    break;
	    
	case ERROR_CONNECTING_TO_ONBOARDEE:
	    Log.d(TAG, "onHandleCommandMessage ERROR_CONNECTING_TO_ONBOARDEE");
	    currentState = State.ERROR_CONNECTING_TO_ONBOARDEE;
	    break; 
	
	case ERROR_WAITING_FOR_ONBOARDEE_ANNOUNCEMENT:
	    Log.d(TAG, "onHandleCommandMessage ERROR_WAITING_FOR_ONBOARDEE_ANNOUNCEMENT");
	    currentState = State.ERROR_WAITING_FOR_ONBOARDEE_ANNOUNCEMENT;
	    break;
	
	case ERROR_ONBOARDEE_ANNOUNCEMENT_RECEIVED:
	    Log.d(TAG, "onHandleCommandMessage ERROR_ONBOARDEE_ANNOUNCEMENT_RECEIVED");
	    currentState = State.ERROR_ONBOARDEE_ANNOUNCEMENT_RECEIVED;
	    break;	    
	
	    
	case ERROR_CONFIGURING_ONBOARDEE:
	    Log.d(TAG, "onHandleCommandMessage ERROR_CONFIGURING_ONBOARDEE");
	    currentState = State.ERROR_CONFIGURING_ONBOARDEE;
	    break;
	
	    
        case ERROR_CONNECTING_TO_TARGET_WIFI_AP:
            Log.d(TAG, "onHandleCommandMessage ERROR_CONNECTING_TO_TARGET_WIFI_AP");
            currentState = State.ERROR_CONNECTING_TO_TARGET_WIFI_AP;
            break;
            
        case ERROR_WAITING_FOR_TARGET_ANNOUNCE:
            Log.d(TAG, "onHandleCommandMessage ERROR_CONNECTING_TO_TARGET_WIFI_AP");
            currentState = State.ERROR_CONNECTING_TO_TARGET_WIFI_AP;
            break;
	default:
	    break;
	}
    }

    /**
     * Move the state machine to a new state.
     * 
     * @param state
     */
    private void setState(State state) {
	Message msg = stateHandler.obtainMessage(state.getValue());
	stateHandler.sendMessage(msg);
    }

    /**
     * Move the state machine to a new state.
     * 
     * @param state
     *            new state
     * @param data
     *            metadata to pass to the new state
     */
    private void setState(State state, Object data) {

	Message msg = stateHandler.obtainMessage(state.getValue());
	msg.obj = data;
	stateHandler.sendMessage(msg);
    }

    /**
     * Calls the OnboardingService API for passing the onboarding configuration
     * to the device.
     * 
     * @param announceData
     *            the Announcement data.
     * @return status of operation.
     */
    private DeviceResponse onboardDevice(final AnnounceData announceData) {
	if (onboardingClient == null) {
	    try {
		if (announceData.serviceName == null) {
		    return new DeviceResponse(ResponseCode.Status_ERROR, "announceData.serviceName == null");
		}
		if (announceData.getPort() == 0) {
		    return new DeviceResponse(ResponseCode.Status_ERROR, "announceData.getPort() == 0");
		}
		onboardingClient = new OnboardingClientImpl(announceData.getServiceName(), bus, new ServiceAvailabilityListener() {
		    @Override
		    public void connectionLost() {
			// expected. we are onboarding the device, hence sending it the another network.
			Log.d(TAG, "onboardDevice connectionLost");
		    }
		}, announceData.getPort());
	    } catch (Exception e) {
		Log.e(TAG, "onboardDevice Exception: ", e);
		return new DeviceResponse(ResponseCode.Status_ERROR);
	    }
	}

	try {
	    ResponseCode connectToDeviceStatus = connectToDevice(onboardingClient).getStatus();
	    if (connectToDeviceStatus != ResponseCode.Status_OK) {
		return new DeviceResponse(ResponseCode.Status_ERROR_CANT_ESTABLISH_SESSION, connectToDeviceStatus.name());
	    }
	    AuthType authType = onboardingConfiguration.getTarget().getAuthType();
	    boolean isPasswordHex = false;
	    String passForConfigureNetwork = onboardingConfiguration.getTarget().getPassword();
	    if (authType == AuthType.WEP) {
		Pair<Boolean, Boolean> wepCheckResult = OnboardingSDKWifiManager.checkWEPPassword(passForConfigureNetwork);
		isPasswordHex = wepCheckResult.second;
	    }
	    Log.d(TAG, "onBoardDevice OnboardingClient isPasswordHex " + isPasswordHex);
	    if (!isPasswordHex) {
		passForConfigureNetwork = toHexadecimalString(onboardingConfiguration.getTarget().getPassword());
		Log.i(TAG, "convert pass to hex: from " + onboardingConfiguration.getTarget().getPassword() + " -> to " + passForConfigureNetwork);
	    }
	    Log.i(TAG, "before configureWiFi networkName = " + onboardingConfiguration.getTarget().getSSID() + " networkPass = " + passForConfigureNetwork + " selectedAuthType = "
		    + onboardingConfiguration.getTarget().getAuthType().getTypeId());
	    onboardingClient.configureWiFi(onboardingConfiguration.getTarget().getSSID(), passForConfigureNetwork, onboardingConfiguration.getTarget().getAuthType().getTypeId());
	    onboardingClient.connectWiFi();
	    return new DeviceResponse(ResponseCode.Status_OK);
	} catch (BusException e) {
	    Log.e(TAG, "onboarddDevice ", e);
	    return new DeviceResponse(ResponseCode.Status_ERROR);
	} catch (Exception e) {
	    Log.e(TAG, "onboarddDevice ", e);
	    return new DeviceResponse(ResponseCode.Status_ERROR);
	}

    }
  
    /**
     * Calls the offboardDevice API for offboarding the device.
     * @param serviceName device's service name 
     * @param port device's application port
     * @return result of action 
     */
    private DeviceResponse offboardDevice(String serviceName, short port) {
	Log.d(TAG, "offboardDevice");
	
	Bundle extras = new Bundle();
	extras.putString(EXTRA_DEVICE_INFORMATION, serviceName);
	extras.putString(EXTRA_NEW_STATE, NewState.CONNECTING_ONBOARDEE.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);
	
	if (onboardingClient == null) {
	    try {
		onboardingClient = new OnboardingClientImpl(serviceName, bus, new ServiceAvailabilityListener() {
		    @Override
		    public void connectionLost() {
			// expected. we are offboarding the device...
			Log.d(TAG, "offboardDevice connectionLost");
		    }
		}, port);
	    } catch (Exception e) {
		Log.e(TAG, "offboardDevice Exception: ", e);
		return new DeviceResponse(ResponseCode.Status_ERROR);
	    }
	}
	
	extras.clear();
	extras.putString(EXTRA_DEVICE_INFORMATION, serviceName);
	extras.putString(EXTRA_NEW_STATE, NewState.CONNECTED_ONBOARDEE.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);
	
	try {
	    ResponseCode connectToDeviceStatus = connectToDevice(onboardingClient).getStatus();
	    if (connectToDeviceStatus != ResponseCode.Status_OK) {
		return new DeviceResponse(ResponseCode.Status_ERROR_CANT_ESTABLISH_SESSION, connectToDeviceStatus.name());
	    }
	    
	    extras.clear();
	    extras.putString(EXTRA_DEVICE_INFORMATION, serviceName);
	    extras.putString(EXTRA_NEW_STATE, NewState.CONFIGURING_ONBOARDEE.toString());
	    sendBroadcast(STATE_CHANGE_ACTION, extras);
	    
	    onboardingClient.offboard();
	    
	    extras.clear();
	    extras.putString(EXTRA_DEVICE_INFORMATION, serviceName);
	    extras.putString(EXTRA_NEW_STATE, NewState.CONFIGURED_ONBOARDEE.toString());
	    sendBroadcast(STATE_CHANGE_ACTION, extras);
	    
	    return new DeviceResponse(ResponseCode.Status_OK);
	} catch (BusException e) {
	    Log.e(TAG, "offboardDevice ", e);
	    return new DeviceResponse(ResponseCode.Status_ERROR);
	} catch (Exception e) {
	    Log.e(TAG, "offboardDevice ", e);
	    return new DeviceResponse(ResponseCode.Status_ERROR);
	}
    }
    
    
    /**
     * Starts an AllJoyn session with another Alljoyn device.
     * 
     * @param client
     * @return status of operation.
     */
    private DeviceResponse connectToDevice(ClientBase client) {
	Log.d(TAG, "connectToDevice ");

	if (client == null) {
	    return new DeviceResponse(ResponseCode.Status_ERROR, "fail connect to device, client == null");
	}
	if (client.isConnected()) {
	    return new DeviceResponse(ResponseCode.Status_OK);
	}

	Status status = client.connect();
	switch (status) {
	case OK:
	    Log.d(TAG, "connectToDevice. Join Session OK");
	    return new DeviceResponse(ResponseCode.Status_OK);
	case ALLJOYN_JOINSESSION_REPLY_ALREADY_JOINED:
	    Log.d(TAG, "connectToDevice: Join Session returned ALLJOYN_JOINSESSION_REPLY_ALREADY_JOINED. Ignoring");
	    return new DeviceResponse(ResponseCode.Status_OK);
	case ALLJOYN_JOINSESSION_REPLY_FAILED:
	case ALLJOYN_JOINSESSION_REPLY_UNREACHABLE:
	    Log.e(TAG, "connectToDevice: Join Session returned ALLJOYN_JOINSESSION_REPLY_FAILED.");
	    return new DeviceResponse(ResponseCode.Status_ERROR_CANT_ESTABLISH_SESSION, "device unreachable");
	default:
	    Log.e(TAG, "connectToDevice: Join session returned error: " + status.name());
	    return new DeviceResponse(ResponseCode.Status_ERROR, "Failed connecting to device");
	}
    }

    /**
     * Converts String in ASCII format to HexAscii.
     * 
     * @param pass
     *            password to convert
     * @return HexAscii of the input.
     */
    private static String toHexadecimalString(String pass) {

	char[] HEX_CODE = "0123456789ABCDEF".toCharArray();
	byte[] data;
	try {
	    data = pass.getBytes("UTF-8");
	} catch (UnsupportedEncodingException e) {
	    Log.e(TAG, "Failed getting bytes of passcode by UTF-8", e);
	    data = pass.getBytes();
	}
	StringBuilder r = new StringBuilder(data.length * 2);
	for (byte b : data) {
	    r.append(HEX_CODE[(b >> 4) & 0xF]);
	    r.append(HEX_CODE[(b & 0xF)]);
	}
	return r.toString();
    }

    /**
     * Start a wifi scan. Broadcasts {@link SCAN_RESULTS_AVAILABLE} intent
     */
    public void scanWiFi() {
	onboardingSDKWifiManager.scan();
    }

    /**
     * @return a list of Wi-Fi networks to which the SDK can onboard a device.
     */
    public List<WiFiNetwork> getCandidateTargetNetworks() {
	return onboardingSDKWifiManager.getNonOnboardableAccessPoints();
    }

    /**
     * @return the current wifi network that the Android device is connected to.
     */
    public WiFiNetwork getCurrentNetwork() {
	Log.d(TAG, "getCurrentNetwork");

	String ssid = onboardingSDKWifiManager.getCurrentSSID();
	Log.d(TAG, "Current SSID is " + ssid);

	if (ssid != null) {
	    return new WiFiNetwork(ssid);
	}

	return null;
    }

    /**
     * Connects the Android device to a WIFI network. Uses STATE_CHANGE and
     * ERROR to return status/progress.
     * 
     * @param network contains detailed data how to connect to the WIFI network.
     * @param connectionTimeout timeout in Msec to complete the task of connecting to a Wi-Fi network
     *           
     */
    public void connectToNetwork(final WiFiNetworkConfiguration network, int connectionTimeout) {

	Log.d(TAG, "connectToNetwork");
	connectToNetworkWifiBroadcastReceiver = new BroadcastReceiver() {

	    @Override
	    public void onReceive(Context arg0, Intent intent) {
		Bundle extras = new Bundle();
		String action = intent.getAction();
		Log.d(TAG, "onReceive action=" + action);

		if (WIFI_CONNECTED_BY_REQUEST_ACTION.equals(action)) {
		    context.unregisterReceiver(connectToNetworkWifiBroadcastReceiver);
		    if (intent.hasExtra(EXTRA_WIFI_WIFICONFIGURATION)) {
			WifiConfiguration config = (WifiConfiguration) intent.getParcelableExtra(EXTRA_WIFI_WIFICONFIGURATION);

			if (OnboardingSDKWifiManager.normalizeSSID(network.getSSID()).equals(OnboardingSDKWifiManager.normalizeSSID(config.SSID))) {
			    extras.clear();
			    extras.putString(EXTRA_NEW_STATE, NewState.CONNECTED_WIFI.toString());
			    sendBroadcast(STATE_CHANGE_ACTION, extras);
			    return;
			}
			extras.clear();
			extras.putString(EXTRA_ERROR_DETAILS, ErrorState.WIFI_TIMEOUT.toString());
			sendBroadcast(ERROR, extras);
		    }
		}

		if (WIFI_TIMEOUT_ACTION.equals(action)) {
		    context.unregisterReceiver(connectToNetworkWifiBroadcastReceiver);
		    extras.clear();
		    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.WIFI_TIMEOUT.toString());
		    sendBroadcast(ERROR, extras);
		}
		if (WIFI_AUTHENTICATION_ERROR.equals(action)) {
		    context.unregisterReceiver(connectToNetworkWifiBroadcastReceiver);
		    extras.clear();
		    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.WIFI_AUTH.toString());
		    sendBroadcast(ERROR, extras);
		}

	    }

	};
	context.registerReceiver(connectToNetworkWifiBroadcastReceiver, wifiIntentFilter);
	Bundle extras = new Bundle();

	extras.putString(EXTRA_NEW_STATE, NewState.CONNECTING_WIFI.toString());
	sendBroadcast(STATE_CHANGE_ACTION, extras);

	if (connectionTimeout < 0) {
	    connectionTimeout = DEFAULT_WIFI_CONNECTION_TIMEOUT;
	}

	onboardingSDKWifiManager.connectToWifiAP(network.getSSID(), network.getAuthType(), network.getPassword(), connectionTimeout);
    }

    /**
     * @return a list of networks that the SDK can onboard .
     */
    public List<WiFiNetwork> getOnboardableDevices() {
	return onboardingSDKWifiManager.getOnboardableAccessPoints();
    }

    /**
     * Starts and resumes the onboarding process. Broadcasts STATE_CHANGE and
     * ERROR intents to communicate status and progress.
     * 
     * @param config
     *            OnboardingConfiguration containing the Onboardee and the
     *            target
     */
    public void runOnboarding(OnboardingConfiguration config) throws IllegalStateException{	
	onboardingConfiguration = config;
	if (currentState==State.IDLE){
	    setState(State.CONNECTING_TO_ONBOARDEE);	   
	}else if (currentState.getValue()>=State.ERROR_CONNECTING_TO_ONBOARDEE.getValue()){
	    switch (currentState) {
	    	case ERROR_CONNECTING_TO_ONBOARDEE:
	    	    setState(State.CONNECTING_TO_ONBOARDEE);	    	    
	    	    break;	    	    	
	    	case ERROR_WAITING_FOR_ONBOARDEE_ANNOUNCEMENT:	    	    
	    	    setState(State.WAITING_FOR_ONBOARDEE_ANNOUNCEMENT);	    
	    	    break;	    	    
	    	case ERROR_ONBOARDEE_ANNOUNCEMENT_RECEIVED:
	    	 throw new IllegalStateException("The device doesn't comply with onboarding service");	    	   	    
	    	case ERROR_CONFIGURING_ONBOARDEE:    	
		    setState(State.CONFIGURING_ONBOARDEE);	   
		    break;		    
	    	case ERROR_CONNECTING_TO_TARGET_WIFI_AP:
	    	    setState(State.CONNECTING_TO_TARGET_WIFI_AP);
	    	    break;	    	
	    	case ERROR_WAITING_FOR_TARGET_ANNOUNCE:
	    	    setState(State.WAITING_FOR_TARGET_ANNOUNCE);
	    	    break;
	    	default:
	    	    break;	    
	    }
	    
	}else{
	    throw new IllegalStateException("onboarding process is already running");
	}
    }

    /**
     * Aborts the onboarding process and returns to original Wi-Fi network.
     */
    public void abortOnboarding() {
	
    }

    /**
     * Offboards a device that appears on the current Wi-Fi network.
     * 
     * @param config
     *            contains the offboarding information needed to complete the
     *            task.
     */
    public void runOffboarding(OffboardingConfiguration config)  throws IllegalStateException,IllegalArgumentException{
	//verify that the OffboardingConfiguration has valid data
	if (config==null || config.getServiceName()==null ||config.getServiceName().isEmpty() || config.getPort()==0){
	    throw new IllegalArgumentException();
	}
	// in case the SDK is in onboarding mode the runOffboarding can't continue
	if (currentState != State.IDLE){
	    throw new IllegalStateException("onboarding process is already running");
	}	
	DeviceResponse deviceResponse=offboardDevice(config.getServiceName(), config.getPort());
	if (deviceResponse.getStatus() != ResponseCode.Status_OK){
	    Bundle extras=new  Bundle();
	    extras.putString(EXTRA_DEVICE_INFORMATION, config.getServiceName());	
	    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.OFFBOARDING_FAILED.toString());
	    sendBroadcast(ERROR,extras);
	}
    }

    /**
     * Checks if the device supports the given service.
     * 
     * @param objectDescriptions
     *            the list of supported services as announced by the device.
     * @param service
     *            name of the service to check
     * @return
     */
    private boolean isSeviceSupported(final BusObjectDescription[] objectDescriptions, String service) {

	if (objectDescriptions != null) {
	    for (int i = 0; i < objectDescriptions.length; i++) {
		String[] interfaces = objectDescriptions[i].getInterfaces();
		for (int j = 0; j < interfaces.length; j++) {
		    String currentInterface = interfaces[j];
		    if (currentInterface.startsWith(service))
			return true;
		}
	    }
	}
	return false;

    }

    /**
     * Starts a timeout Announcement to arrive from a device. Takes the timeout
     * interval from the {@link OnboardingConfiguration} that stores the data.
     * If timeout expires, moves the state machine to idle state and sends
     * timeout intent.
     * 
     * @return true if in correct state else false.
     */
    private boolean startAnnouncementTimeout() {

	Log.d(TAG, "startAnnouncementTimeout ");
	long timeout = 0;
	switch (currentState) {
	case WAITING_FOR_ONBOARDEE_ANNOUNCEMENT:
	    timeout = onboardingConfiguration.getOnboardeeAnnoucementTimeout();
	    break;
	case WAITING_FOR_TARGET_ANNOUNCE:
	    timeout = onboardingConfiguration.getTargetAnnoucementTimeout();
	    break;
	default:
	    Log.e(TAG, "startAnnouncementTimeout has been intialized in bad state abort");
	    return false;
	}

	announcementTimeout.schedule(new TimerTask() {

	    Bundle extras = new Bundle();

	    @Override
	    public void run() {

		Log.e(TAG, "Time out Expired  " + currentState.toString());
		switch (currentState) {
		case WAITING_FOR_ONBOARDEE_ANNOUNCEMENT: {
		    extras.clear();
		    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.FIND_ONBOARDEE_TIMEOUT.toString());
		    sendBroadcast(ERROR, extras);
		    setState(State.ERROR_WAITING_FOR_ONBOARDEE_ANNOUNCEMENT);

		}
		    break;

		case WAITING_FOR_TARGET_ANNOUNCE: {
		    extras.clear();
		    extras.putString(EXTRA_ERROR_DETAILS, ErrorState.VERIFICATION_TIMEOUT.toString());
		    sendBroadcast(ERROR, extras);
		    setState(State.ERROR_WAITING_FOR_TARGET_ANNOUNCE);
		}
		    break;
		default:
		    break;
		}

	    }
	}, timeout);
	return true;
    }

    /**
     * Stop the Announcement Timeout timer that was activated by
     * startAnnouncementTimeout.
     */
    private void stopAnnouncementTimeout() {
	announcementTimeout.cancel();
	announcementTimeout.purge();
	announcementTimeout = new Timer();
    }

    /**
     * A wrapper method that deals with sending intent broadcasts with extra
     * data if exists.
     * 
     * @param action
     *            an action for the intent
     * @param extras
     *            extras for the intent
     */
    private void sendBroadcast(String action, Bundle extras) {

	Intent intent = new Intent(action);
	if (extras != null && !extras.isEmpty()) {
	    intent.putExtras(extras);
	}
	context.sendBroadcast(intent);
    }

}
