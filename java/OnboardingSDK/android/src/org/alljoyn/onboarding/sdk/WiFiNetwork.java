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

import org.alljoyn.onboarding.OnboardingService.AuthType;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * WiFiNetwork encapsulates information retrieved from a WIFI scan
 */
public class WiFiNetwork implements Parcelable {

    /**
     * WIFI SSID name
     */
    protected String SSID = null;
    /**
     * WIFI AuthType
     */
    protected AuthType authType;
    /**
     * WIFI signal strength
     */
    protected int level = 0;

    /**
     * Constructor
     */
    public WiFiNetwork() {
    }

    /**
     * Constructor with SSID
     * 
     * @param SSID
     */
    public WiFiNetwork(String SSID) {
	this.SSID = SSID;
    }

    /**
     * Constructor with SSID,capabilities,level
     * 
     * @param SSID
     * @param capablities
     * @param level
     */
    public WiFiNetwork(String SSID, String capabilities, int level) {
	this.SSID = SSID;
	this.authType = capabilitiesToAuthType(capabilities);
	this.level = level;
    }

    @Override
    public int describeContents() {
	return 0;
    }

    @Override
    /**
     * write the members to the Parcel
     */
    public void writeToParcel(Parcel dest, int flags) {
	dest.writeString(SSID);
	dest.writeInt(authType.getTypeId());
	dest.writeInt(level);
    }

    /**
     * Constructor from a Parcel
     * 
     * @param in
     */
    public WiFiNetwork(Parcel in) {
	this.SSID = in.readString();
	this.authType = AuthType.getAuthTypeById((short) in.readInt());
	this.level = in.readInt();
    }

    public static final Parcelable.Creator<WiFiNetwork> CREATOR = new Parcelable.Creator<WiFiNetwork>() {

	public WiFiNetwork createFromParcel(Parcel in) {
	    return new WiFiNetwork(in);
	}

	public WiFiNetwork[] newArray(int size) {
	    return new WiFiNetwork[size];
	}
    };

    /**
     * Getter of SSID
     * 
     * @return
     */
    public String getSSID() {
	return SSID;
    }

    /**
     * Setter of SSID
     * 
     * @param SSID
     */
    public void setSSID(String SSID) {
	this.SSID = SSID;
    }

    /**
     * Getter of AuthType
     * 
     * @return
     */
    public AuthType getAuthType() {
	return authType;
    }

    /**
     * Getter of level
     * 
     * @return
     */
    public int getLevel() {
	return level;
    }

    private AuthType capabilitiesToAuthType(String capabilities) {
	if (capabilities.contains("WPA2")) {
	    return AuthType.WPA2_AUTO;
	}
	if (capabilities.contains("WPA")) {
	    return AuthType.WPA_AUTO;
	}
	if (capabilities.contains(AuthType.WEP.name())) {
	    return AuthType.WEP;
	}
	return AuthType.OPEN;
    }

}
