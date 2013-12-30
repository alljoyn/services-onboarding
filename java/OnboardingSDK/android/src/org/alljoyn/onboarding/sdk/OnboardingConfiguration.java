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


/**
 * OnboardingConfiguration encapsulates onboarding configuration data to onboard
 * a AllJoyn device on a target host
 * it includes onboardee WIFI credentials,connection timeout,announcement timeout  
 *             target WIFI credentials,connection timeout,announcement timeout             
 */
public class OnboardingConfiguration {
    
    /**
     * Stores the onboardee WIFI credentials.
     */
    private WiFiNetworkConfiguration onboardee = null;
    
    /**
     * Stores the target WIFI credentials.
     */
    private WiFiNetworkConfiguration target = null;
    
    /**
     * Stores the timeout of establishing WIFI connection with the onboardee in msec.
     */
    private long onboardeeConnectionTimeout = OnboardingSDK.DEFAULT_WIFI_CONNECTION_TIMEOUT;
    
    /**
     * Stores the timeout of waiting for announcement from the onboardee after
     * establishing WIFI connection with the onboardee in msec.
     */
    private long onboardeeAnnoucementTimeout = OnboardingSDK.DEFAULT_ANNOUNCEMENT_TIMEOUT;
    
    /**
     * Stores the timeout of establishing WIFI connection with the target in msec.
     */
    private long targetConnectionTimeout = OnboardingSDK.DEFAULT_WIFI_CONNECTION_TIMEOUT;
    
    /**
     * Stores the timeout of waiting for announcement from the onboardee after
     * establishing WIFI connection with the target in msec.
     */
    private long targetAnnoucementTimeout = OnboardingSDK.DEFAULT_ANNOUNCEMENT_TIMEOUT;

    /**
     * Getter of onboardee WiFiNetworkConfiguration
     * @return
     */
    public WiFiNetworkConfiguration getOnboardee() {
	return onboardee;
    }

    /**
     * Setter of onboardee WiFiNetworkConfiguration
     * @param onboardee
     */
    public void setOnboardee(WiFiNetworkConfiguration onboardee) {
	this.onboardee = onboardee;
    }

    /**
     * Getter of target WiFiNetworkConfiguration
     * @return
     */
    public WiFiNetworkConfiguration getTarget() {
	return target;
    }

    /**
     * Setter of target WiFiNetworkConfiguration
     * @param target
     */
    public void setTarget(WiFiNetworkConfiguration target) {
	this.target = target;
    }

    /**
     * Getter of onboardeeConnectionTimeout
     * @return
     */
    public long getOnboardeeConnectionTimeout() {
	return onboardeeConnectionTimeout;
    }

    /**
     * Setter of onboardeeConnectionTimeout
     * @param onboardeeConnectionTimeout
     */
    public void setOnboardeeConnectionTimeout(long onboardeeConnectionTimeout) {
	this.onboardeeConnectionTimeout = onboardeeConnectionTimeout;
    }

    /**
     * Getter of onboardeeConnectionTimeout
     * @return
     */
    public long getOnboardeeAnnoucementTimeout() {
	return onboardeeAnnoucementTimeout;
    }

    /**
     * Setter of onboardeeAnnoucementTimeout
     * @param onbaordeeAnnoucementTimeout
     */
    public void setOnboardeeAnnoucementTimeout(long onbaordeeAnnoucementTimeout) {
	this.onboardeeAnnoucementTimeout = onbaordeeAnnoucementTimeout;
    }

    /**
     * Getter of targetConnectionTimeout
     * @return
     */
    public long getTargetConnectionTimeout() {
	return targetConnectionTimeout;
    }

    /**
     * Setter of targetConnectionTimeout
     * @param targetConnectionTimeout
     */
    public void setTargetConnectionTimeout(long targetConnectionTimeout) {
	this.targetConnectionTimeout = targetConnectionTimeout;
    }

    /**
     * Getter of targetAnnoucementTimeout
     * @return
     */
    public long getTargetAnnoucementTimeout() {
	return targetAnnoucementTimeout;
    }

    /**
     * Setter of targetAnnoucementTimeout
     * @param targetAnnoucementTimeout
     */
    public void setTargetAnnoucementTimeout(long targetAnnoucementTimeout) {
	this.targetAnnoucementTimeout = targetAnnoucementTimeout;
    }

    /**
     * Constructor of OnboardingConfiguration that receives all parameters including WIFI credentials and timeouts.
     * @param onboardee 
     * @param onboardeeConnectionTimeoutMsec
     * @param onboardeeAnnoucementTimeoutMsec
     * @param target
     * @param targetConnectionTimeoutMsec
     * @param targetAnnoucementTimeoutMsec
     */
    public OnboardingConfiguration(WiFiNetworkConfiguration onboardee, long onboardeeConnectionTimeoutMsec, long onboardeeAnnoucementTimeoutMsec, WiFiNetworkConfiguration target,
	    long targetConnectionTimeoutMsec, long targetAnnoucementTimeoutMsec) {
	this.onboardee = onboardee;
	this.target = target;
	this.onboardeeConnectionTimeout = onboardeeConnectionTimeoutMsec;
	this.onboardeeAnnoucementTimeout = onboardeeAnnoucementTimeoutMsec;
	this.targetConnectionTimeout = targetConnectionTimeoutMsec;
	this.targetAnnoucementTimeout = targetAnnoucementTimeoutMsec;
    }

    /**
     * Constructor of OnboardingConfiguration that receives  WIFI credentials and uses default timeout values
     * @param onboardee
     * @param target
     */
    public OnboardingConfiguration(WiFiNetworkConfiguration onboardee, WiFiNetworkConfiguration target) {
	this.onboardee = onboardee;
	this.target = target;
	this.onboardeeConnectionTimeout = 20000;
	this.onboardeeAnnoucementTimeout = 20000;
	this.targetConnectionTimeout = 20000;
	this.targetAnnoucementTimeout = 20000;
    }

}
