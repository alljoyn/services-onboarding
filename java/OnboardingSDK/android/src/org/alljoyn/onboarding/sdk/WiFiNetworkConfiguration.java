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
 * WiFiNetwork encapsulates information how to connect to a WIFI network
 * 
 */
public class WiFiNetworkConfiguration extends WiFiNetwork implements Parcelable {

    /**
     * WIFI password
     */
    private String password = null;
    /**
     * WIFI cipher
     */
    private String cipher = null;

    @Override
    public int describeContents() {
	return 0;
    }

    @Override
    /**
     *  write the members to the Parcel
     */
    public void writeToParcel(Parcel dest, int flags) {
	dest.writeString(SSID);
	dest.writeString(password);
	dest.writeString(cipher);
	dest.writeInt(authType.getTypeId());
    }

    /**
     * Constructor with SSID,authType,password,cipher
     * 
     * @param SSID
     * @param authType
     * @param password
     * @param cipher
     */
    public WiFiNetworkConfiguration(String SSID, AuthType authType,
	    String password, String cipher) {
	super.setSSID(SSID);
	this.password = password;
	this.authType = authType;
	this.cipher = cipher;
    }

    /**
     * Constructor from a Parcel
     * 
     * @param in
     */
    public WiFiNetworkConfiguration(Parcel in) {
	SSID = in.readString();
	password = in.readString();
	cipher = in.readString();
	authType = AuthType.getAuthTypeById((short) in.readInt());

    }

    public static final Parcelable.Creator<WiFiNetworkConfiguration> CREATOR = new Parcelable.Creator<WiFiNetworkConfiguration>() {
	public WiFiNetworkConfiguration createFromParcel(Parcel in) {
	    return new WiFiNetworkConfiguration(in);
	}

	public WiFiNetworkConfiguration[] newArray(int size) {
	    return new WiFiNetworkConfiguration[size];
	}
    };

    /**
     * Getter of password
     * 
     * @return
     */
    public String getPassword() {
	return password;
    }

    /**
     * Getter of authType
     * 
     * @return
     */
    public AuthType getAuthType() {
	return authType;
    }

}
