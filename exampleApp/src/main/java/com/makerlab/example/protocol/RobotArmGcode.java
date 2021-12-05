package com.makerlab.example.protocol;

import java.io.UnsupportedEncodingException;

/*
    https://www.thingiverse.com/thing:1718984
    https://en.wikipedia.org/wiki/G-code
 */
public class RobotArmGcode {
    static final String LINE_BREAK="\r\n";
    //
    static final String M17 = "M17"; //stepper on
    static final String M18 = "M18"; //stepper off
    static final String M3 = "M3"; //grepper on
    static final String M5 = "M5"; //grepper off
    static final String G1 = "G1 ";   // move steppers
    //
    static final String HOME = G1+"X0 Y180 Z180";
    static final String  REST = G1+"X0 Y40 Z70";
    static final String BOTTOM = G1+"X0 Y100 Z0";
    static final String END_STOP =G1+"X0 Y19.5 Z134";

    private int mPosX=0, mPosY=180,mPosZ=180;

    private byte[] getPayload(String gcode) {
        try {
            gcode+=LINE_BREAK;
            return gcode.getBytes("iso8859-1");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
