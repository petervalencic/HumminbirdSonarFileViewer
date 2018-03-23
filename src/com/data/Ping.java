/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.data;

/**
 *
 * @author Peter
 */
public interface Ping {
	byte[] getSoundings();
	float getLowLimit();
	float getTemp();
	float getDepth();
	int getTimeStamp();
	float getSpeed();
	float getTrack();
	double getLongitude();
	double getLatitude();
}
